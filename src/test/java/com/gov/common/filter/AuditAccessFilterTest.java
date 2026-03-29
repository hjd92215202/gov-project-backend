package com.gov.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

/**
 * 职责：验证审计过滤器的用户识别链路与 IP 解析逻辑。
 * 为什么存在：审计日志若无法稳定定位操作者，会直接影响审计追溯可信度。
 * 关键输入输出：输入为请求属性、登录态和请求头，输出为解析后的 userId 与 clientIp。
 * 关联链路：AuditAccessFilter -> sys_audit_log -> 审计页面。
 */
class AuditAccessFilterTest {

    private final AuditAccessFilter filter = new AuditAccessFilter();

    /**
     * 作用：当请求属性中已有 userId 时，优先使用属性值，避免链路尾部丢失上下文。
     */
    @Test
    void resolveUserId_shouldPreferRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("audit.userId", "123");

        String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

        assertEquals("123", userId);
    }

    /**
     * 作用：当当前线程存在登录态时，应直接返回当前登录用户ID。
     */
    @Test
    void resolveUserId_shouldUseCurrentLoginId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(88L);

            String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

            assertEquals("88", userId);
        }
    }

    /**
     * 作用：当当前线程登录态为空时，应回退到 token 反查用户ID。
     */
    @Test
    void resolveUserId_shouldFallbackToTokenLookup() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "token-abc");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);
            stp.when(StpUtil::getTokenName).thenReturn("Authorization");
            stp.when(() -> StpUtil.getLoginIdByToken("token-abc")).thenReturn(99L);

            String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

            assertEquals("99", userId);
        }
    }

    /**
     * 作用：支持 Bearer Token 头，确保前端常见写法可被识别。
     */
    @Test
    void resolveUserId_shouldSupportBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-xyz");
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);
            stp.when(StpUtil::getTokenName).thenReturn("Authorization");
            stp.when(() -> StpUtil.getLoginIdByToken("token-xyz")).thenReturn(100L);

            String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

            assertEquals("100", userId);
        }
    }

    /**
     * 作用：当无法识别用户时，统一返回 anonymous。
     */
    @Test
    void resolveUserId_shouldReturnAnonymousWhenNoIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(null);
            stp.when(StpUtil::getTokenName).thenReturn("Authorization");

            String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

            assertEquals("anonymous", userId);
        }
    }

    /**
     * 作用：优先从 X-Forwarded-For 提取首个 IP 作为真实客户端地址。
     */
    @Test
    void resolveClientIp_shouldUseFirstForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.setRemoteAddr("127.0.0.1");

        String clientIp = ReflectionTestUtils.invokeMethod(filter, "resolveClientIp", request);

        assertEquals("10.0.0.1", clientIp);
    }
}
