package com.gov.common.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 职责：为每个请求生成并传播 traceId。
 * 为什么存在：便于跨层定位问题，把同一请求的日志串起来，支持审计与运维排障。
 * 关键输入输出：输入为请求头 X-Trace-Id（可选），输出为 MDC 与响应头中的 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = normalizeTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String normalizeTraceId(String incomingTraceId) {
        if (incomingTraceId == null || incomingTraceId.trim().isEmpty()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return incomingTraceId.trim();
    }
}
