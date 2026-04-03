package com.gov.common.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 职责：基于令牌桶算法对每个 IP 进行限流，防止单个客户端打垮服务。
 * 为什么存在：100人并发场景下，若某个客户端异常重试或恶意刷接口，
 * 会挤占其他用户的连接池和线程资源，需要在入口层做第一道防护。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${gov.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${gov.rate-limit.max-requests-per-second:30}")
    private int maxRequestsPerSecond;

    @Value("${gov.rate-limit.burst-capacity:60}")
    private int burstCapacity;

    // IP -> [tokens, lastRefillTime]
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String uri = request.getRequestURI();
        // 健康检查不限流
        return uri != null && (uri.contains("/health/live") || uri.contains("/health/ready"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        if (!tryAcquire(ip)) {
            log.warn("限流触发 ip={} uri={}", ip, request.getRequestURI());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{"code":429,"msg":"请求过于频繁，请稍后再试","data":null}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tryAcquire(String ip) {
        long now = System.currentTimeMillis();
        long[] bucket = buckets.computeIfAbsent(ip, k -> new long[]{burstCapacity, now});

        synchronized (bucket) {
            long elapsed = now - bucket[1];
            // 按时间补充令牌
            long refill = (long) (elapsed / 1000.0 * maxRequestsPerSecond);
            if (refill > 0) {
                bucket[0] = Math.min(burstCapacity, bucket[0] + refill);
                bucket[1] = now;
            }
            if (bucket[0] > 0) {
                bucket[0]--;
                return true;
            }
            return false;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
