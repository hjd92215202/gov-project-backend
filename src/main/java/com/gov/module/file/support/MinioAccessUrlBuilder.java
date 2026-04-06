package com.gov.module.file.support;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * MinIO 访问地址构建器。
 * 用于把容器内部地址统一改写成浏览器可访问的公网地址。
 */
@Component
public class MinioAccessUrlBuilder {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-endpoint:${minio.endpoint}}")
    private String publicEndpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 将内部 MinIO 地址改写为对外访问地址。
     */
    public String rewriteToPublicUrl(String accessUrl) {
        if (StrUtil.isBlank(accessUrl)) {
            return "";
        }
        String normalizedInternalEndpoint = normalizeEndpoint(endpoint);
        String normalizedPublicEndpoint = normalizeEndpoint(resolvePublicEndpoint());
        if (StrUtil.isBlank(normalizedInternalEndpoint) || StrUtil.equals(normalizedInternalEndpoint, normalizedPublicEndpoint)) {
            return accessUrl;
        }
        String internalPrefix = normalizedInternalEndpoint + "/";
        if (StrUtil.startWith(accessUrl, internalPrefix)) {
            return normalizedPublicEndpoint + accessUrl.substring(normalizedInternalEndpoint.length());
        }
        return accessUrl;
    }

    /**
     * 基于公开地址拼装对象访问路径。
     */
    public String buildPublicObjectUrl(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            return "";
        }
        return normalizeEndpoint(resolvePublicEndpoint()) + "/" + bucketName + "/" + objectName;
    }

    public String buildPublicDownloadUrl(String objectName, String fileName) {
        String objectUrl = buildPublicObjectUrl(objectName);
        if (StrUtil.isBlank(objectUrl)) {
            return "";
        }
        return objectUrl + "?response-content-disposition=" + encode(buildDownloadContentDisposition(fileName));
    }

    public String buildDownloadContentDisposition(String fileName) {
        String normalizedFileName = StrUtil.blankToDefault(StrUtil.trim(fileName), "download");
        String asciiFallback = buildAsciiFallbackFileName(normalizedFileName);
        String encodedUtf8FileName = encode(normalizedFileName);
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encodedUtf8FileName;
    }

    private String resolvePublicEndpoint() {
        return StrUtil.blankToDefault(publicEndpoint, endpoint);
    }

    private String normalizeEndpoint(String value) {
        return StrUtil.removeSuffix(StrUtil.trim(value), "/");
    }

    private String buildAsciiFallbackFileName(String fileName) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fileName.length(); i++) {
            char current = fileName.charAt(i);
            if (current >= 32 && current <= 126 && current != '"' && current != '\\') {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        return builder.length() == 0 ? "download" : builder.toString();
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(StrUtil.blankToDefault(value, ""), StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not available", e);
        }
    }
}
