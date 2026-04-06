package com.gov.module.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.file.entity.SysFile;
import com.gov.module.file.mapper.SysFileMapper;
import com.gov.module.file.service.SysFileService;
import com.gov.module.file.support.MinioAccessUrlBuilder;
import com.gov.module.project.dto.ProjectAttachmentDTO;
import com.gov.module.project.vo.ProjectFileVO;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件服务实现。
 * 这里把对象存储和数据库元数据管理放在一起，
 * 避免控制器层散落文件上传、签名地址和绑定关系逻辑。
 */
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private static final String PROJECT_FILE_DOWNLOAD_PATH_PREFIX = "/api/project/file/download/";

    private static final Set<String> IMAGE_EXTENSIONS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    )));

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioAccessUrlBuilder minioAccessUrlBuilder;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public ProjectFileVO uploadProjectFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFileName = StrUtil.blankToDefault(file.getOriginalFilename(), "未命名文件");
        String objectName = buildObjectName(originalFileName);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("文件上传失败：" + e.getMessage(), e);
        }

        SysFile sysFile = new SysFile();
        sysFile.setBizId(null);
        sysFile.setFileName(originalFileName);
        sysFile.setFilePath(objectName);
        sysFile.setFileType(resolveFileType(file, originalFileName));
        sysFile.setFileSize(file.getSize());
        save(sysFile);
        return toProjectFileVO(sysFile);
    }

    @Override
    public List<ProjectFileVO> listProjectFiles(Long projectId) {
        if (projectId == null) {
            return new ArrayList<ProjectFileVO>();
        }
        List<SysFile> files = lambdaQuery()
                .eq(SysFile::getBizId, projectId)
                .orderByAsc(SysFile::getCreateTime)
                .list();
        return files.stream().map(this::toProjectFileVO).collect(Collectors.toList());
    }

    @Override
    public void syncProjectFiles(Long projectId, List<ProjectAttachmentDTO> attachments) {
        if (projectId == null) {
            return;
        }

        Set<Long> targetIds = attachments == null
                ? new LinkedHashSet<Long>()
                : attachments.stream()
                .map(ProjectAttachmentDTO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet<Long>::new));

        List<SysFile> currentFiles = lambdaQuery()
                .eq(SysFile::getBizId, projectId)
                .list();
        Set<Long> currentIds = currentFiles.stream()
                .map(SysFile::getId)
                .collect(Collectors.toSet());

        if (!targetIds.isEmpty()) {
            List<SysFile> targetFiles = listByIds(targetIds);
            if (targetFiles.size() != targetIds.size()) {
                throw new IllegalArgumentException("存在无效的项目附件，请重新上传后再试");
            }
            for (SysFile file : targetFiles) {
                if (file.getBizId() != null && !Objects.equals(file.getBizId(), projectId)) {
                    throw new IllegalArgumentException("存在已绑定其他项目的附件，请刷新页面后重试");
                }
            }
            List<SysFile> needBindFiles = targetFiles.stream()
                    .filter(file -> !Objects.equals(file.getBizId(), projectId))
                    .collect(Collectors.toList());
            if (!needBindFiles.isEmpty()) {
                needBindFiles.forEach(item -> item.setBizId(projectId));
                updateBatchById(needBindFiles);
            }
        }

        List<SysFile> needRemoveFiles = currentFiles.stream()
                .filter(file -> !targetIds.contains(file.getId()))
                .collect(Collectors.toList());
        if (!needRemoveFiles.isEmpty()) {
            removeFiles(needRemoveFiles);
        }

        if (targetIds.isEmpty() && !currentIds.isEmpty()) {
            return;
        }
    }

    @Override
    public void removeProjectFiles(Long projectId) {
        if (projectId == null) {
            return;
        }
        List<SysFile> files = lambdaQuery()
                .eq(SysFile::getBizId, projectId)
                .list();
        removeFiles(files);
    }

    @Override
    public int cleanupTemporaryFiles(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return 0;
        }
        List<SysFile> files = lambdaQuery()
                .in(SysFile::getId, fileIds)
                .isNull(SysFile::getBizId)
                .list();
        if (files.isEmpty()) {
            return 0;
        }
        removeFiles(files);
        return files.size();
    }

    @Override
    public int cleanupExpiredTemporaryFiles(Date deadline) {
        if (deadline == null) {
            return 0;
        }
        List<SysFile> files = lambdaQuery()
                .isNull(SysFile::getBizId)
                .lt(SysFile::getCreateTime, deadline)
                .list();
        if (files.isEmpty()) {
            return 0;
        }
        removeFiles(files);
        return files.size();
    }

    private void removeFiles(List<SysFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (SysFile file : files) {
            deleteObjectQuietly(file.getFilePath());
        }
        removeByIds(files.stream().map(SysFile::getId).collect(Collectors.toList()));
    }

    private void deleteObjectQuietly(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception ignored) {
            // 对象存储清理失败不应影响主流程，数据库记录仍会正常删除。
        }
    }

    private ProjectFileVO toProjectFileVO(SysFile file) {
        ProjectFileVO vo = new ProjectFileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileType(file.getFileType());
        vo.setFileSize(file.getFileSize());
        vo.setImage(isImageFile(file.getFileType(), file.getFileName()));
        String previewUrl = buildPreviewUrl(file.getFilePath());
        vo.setPreviewUrl(previewUrl);
        vo.setDownloadUrl(buildDownloadUrl(file.getId()));
        vo.setAccessUrl(previewUrl);
        return vo;
    }

    private String buildObjectName(String originalFileName) {
        String suffix = FileUtil.getSuffix(originalFileName);
        LocalDate currentDate = LocalDate.now();
        StringBuilder builder = new StringBuilder("project/")
                .append(currentDate.getYear()).append("/")
                .append(String.format("%02d", currentDate.getMonthValue())).append("/")
                .append(String.format("%02d", currentDate.getDayOfMonth())).append("/")
                .append(IdUtil.simpleUUID());
        if (StrUtil.isNotBlank(suffix)) {
            builder.append(".").append(suffix);
        }
        return builder.toString();
    }

    private String resolveFileType(MultipartFile file, String originalFileName) {
        if (StrUtil.isNotBlank(file.getContentType())) {
            return file.getContentType();
        }
        String suffix = FileUtil.getSuffix(originalFileName);
        return StrUtil.isNotBlank(suffix) ? suffix.toLowerCase() : "application/octet-stream";
    }

    private boolean isImageFile(String fileType, String fileName) {
        if (StrUtil.startWithIgnoreCase(fileType, "image/")) {
            return true;
        }
        String suffix = FileUtil.getSuffix(fileName);
        return StrUtil.isNotBlank(suffix) && IMAGE_EXTENSIONS.contains(suffix.toLowerCase());
    }

    private String buildPreviewUrl(String objectName) {
        return buildAccessUrl(objectName, null, null);
    }

    private String buildDownloadUrl(Long fileId) {
        if (fileId == null) {
            return "";
        }
        return PROJECT_FILE_DOWNLOAD_PATH_PREFIX + fileId;
    }

    private String buildAccessUrl(String objectName, Map<String, String> extraQueryParams, String fallbackUrl) {
        if (StrUtil.isBlank(objectName)) {
            return "";
        }
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(2, TimeUnit.HOURS);
            if (extraQueryParams != null && !extraQueryParams.isEmpty()) {
                builder.extraQueryParams(extraQueryParams);
            }
            String accessUrl = minioClient.getPresignedObjectUrl(builder.build());
            return minioAccessUrlBuilder.rewriteToPublicUrl(accessUrl);
        } catch (Exception ignored) {
            return StrUtil.blankToDefault(fallbackUrl, minioAccessUrlBuilder.buildPublicObjectUrl(objectName));
        }
    }
}
