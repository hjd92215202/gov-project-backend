package com.gov.module.file.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.common.result.R;
import com.gov.module.file.support.MinioAccessUrlBuilder;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * 文件上传控制器。
 * 当前项目保留了一个通用 MinIO 上传入口，主要给图片或资料附件上传使用。
 */
@Api(tags = "文件上传管理")
@RestController
@RequestMapping("/common")
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioAccessUrlBuilder minioAccessUrlBuilder;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传单个文件到 MinIO 并返回可访问地址。
     */
    @ApiOperation(value = "上传文件(图片或压缩包)", notes = "请通过 form-data 格式上传")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        try {
            String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "未命名文件");
            String suffix = FileUtil.getSuffix(originalFilename);
            String objectName = IdUtil.simpleUUID();
            if (StrUtil.isNotBlank(suffix)) {
                objectName = objectName + "." + suffix;
            }

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url;
            try {
                url = minioAccessUrlBuilder.rewriteToPublicUrl(minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(2, TimeUnit.HOURS)
                        .build()));
            } catch (Exception exception) {
                LOGGER.warn("生成上传文件预签名地址失败 objectName={} message={}，回退为公共访问地址",
                        objectName, exception.getMessage());
                url = minioAccessUrlBuilder.buildPublicObjectUrl(objectName);
            }
            return R.ok(url, "上传成功");
        } catch (Exception e) {
            return R.fail("文件上传失败：" + e.getMessage());
        }
    }
}
