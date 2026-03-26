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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @ApiOperation(value = "上传文件(图片或ZIP)", notes = "请通过 form-data 格式上传")
    @PostMapping(value = "/upload", consumes = "multipart/form-data") // 显式声明接收格式
    public R<String> upload(@RequestPart("file") MultipartFile file) { // 使用 @RequestPart
        if (file.isEmpty()) {
            return R.fail("上传文件不能为空");
        }

        try {
            // 1. 获取文件名和后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = FileUtil.getSuffix(originalFilename);

            // 2. 生成在 MinIO 里的唯一文件名
            String objectName = IdUtil.simpleUUID() + "." + suffix;

            // 3. 上传到 MinIO
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            // 4. 返回完整访问路径 (本地开发：http://127.0.0.1:9000/gov-files/xxx.jpg)
            String url = endpoint + "/" + bucketName + "/" + objectName;
            return R.ok(url, "上传成功");

        } catch (Exception e) {
            return R.fail("文件上传失败：" + e.getMessage());
        }
    }
}