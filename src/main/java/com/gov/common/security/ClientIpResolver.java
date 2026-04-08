package com.gov.common.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统一解析客户端来源 IP，只在请求来自可信代理时才信任转发头。
 */
@Component
public class ClientIpResolver {

    @Value("${gov.security.trusted-proxies:}")
    private String trustedProxyConfig;

    private volatile List<String> trustedProxySources = Collections.emptyList();

    @PostConstruct
    public void init() {
        List<String> parsed = new ArrayList<String>();
        if (StrUtil.isNotBlank(trustedProxyConfig)) {
            String[] parts = trustedProxyConfig.split(",");
            for (String part : parts) {
                String normalized = StrUtil.trimToEmpty(part);
                if (StrUtil.isNotBlank(normalized)) {
                    parsed.add(normalized);
                }
            }
        }
        trustedProxySources = Collections.unmodifiableList(parsed);
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String remoteAddr = StrUtil.blankToDefault(StrUtil.trimToEmpty(request.getRemoteAddr()), "unknown");
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            String[] values = forwardedFor.split(",");
            for (String value : values) {
                String candidate = normalizeCandidate(value);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        String realIp = normalizeCandidate(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return remoteAddr;
    }

    public boolean isTrustedProxy(String remoteAddr) {
        String normalizedRemoteAddr = StrUtil.trimToEmpty(remoteAddr);
        if (StrUtil.isBlank(normalizedRemoteAddr) || trustedProxySources.isEmpty()) {
            return false;
        }
        for (String source : trustedProxySources) {
            if (matchesTrustedSource(normalizedRemoteAddr, source)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeCandidate(String value) {
        String candidate = StrUtil.trimToNull(value);
        if (candidate == null || "unknown".equalsIgnoreCase(candidate)) {
            return null;
        }
        return candidate;
    }

    private boolean matchesTrustedSource(String remoteAddr, String source) {
        if (StrUtil.isBlank(source)) {
            return false;
        }
        if (!source.contains("/")) {
            return source.equals(remoteAddr);
        }

        try {
            String[] parts = source.split("/", 2);
            InetAddress address = InetAddress.getByName(remoteAddr);
            InetAddress networkAddress = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            byte[] addressBytes = address.getAddress();
            byte[] networkBytes = networkAddress.getAddress();
            if (addressBytes.length != networkBytes.length) {
                return false;
            }

            int totalBits = addressBytes.length * 8;
            if (prefixLength < 0 || prefixLength > totalBits) {
                return false;
            }

            BigInteger addressInt = new BigInteger(1, addressBytes);
            BigInteger networkInt = new BigInteger(1, networkBytes);
            int shiftBits = totalBits - prefixLength;
            if (shiftBits == 0) {
                return addressInt.equals(networkInt);
            }
            return addressInt.shiftRight(shiftBits).equals(networkInt.shiftRight(shiftBits));
        } catch (Exception exception) {
            return false;
        }
    }
}
