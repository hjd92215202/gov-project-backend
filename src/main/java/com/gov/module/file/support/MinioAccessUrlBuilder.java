package com.gov.module.file.support;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    private String resolvePublicEndpoint() {
        return StrUtil.blankToDefault(publicEndpoint, endpoint);
    }

    private String normalizeEndpoint(String value) {
        return StrUtil.removeSuffix(StrUtil.trim(value), "/");
    }
}
