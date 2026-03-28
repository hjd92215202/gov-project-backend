package com.gov.module.file.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.gov.common.result.R;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器。
 * 目前项目只开放了一个 MinIO 上传入口，主要给项目图片或附件上传使用。
 */
@Api(tags = "文件上传管理")
@RestController
@RequestMapping("/common")
public class FileController {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    /**
     * 上传单个文件到 MinIO 并返回可访问地址。
     */
    @ApiOperation(value = "上传文件(图片或 ZIP)", notes = "请通过 form-data 格式上传")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public R<String> upload(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);
            String objectName = IdUtil.simpleUUID() + "." + suffix;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            String url = endpoint + "/" + bucketName + "/" + objectName;
            return R.ok(url, "上传成功");
        } catch (Exception e) {
            return R.fail("文件上传失败：" + e.getMessage());
        }
    }
}
