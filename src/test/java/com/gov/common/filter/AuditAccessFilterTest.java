package com.gov.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

/**
 * 职责：验证审计过滤器的用户识别逻辑。
 * 为什么存在：审计日志是否能正确落到真实用户，直接影响审计可追溯性。
 * 关键输入输出：输入为当前登录态与请求头 token，输出为审计 userId。
 */
class AuditAccessFilterTest {

    private final AuditAccessFilter filter = new AuditAccessFilter();

    /**
     * 作用：优先使用当前登录态中的用户 ID。
     */
    @Test
    void resolveUserId_shouldUseCurrentLoginIdFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdDefaultNull).thenReturn(88L);

            String userId = ReflectionTestUtils.invokeMethod(filter, "resolveUserId", request);

            assertEquals("88", userId);
        }
    }

    /**
     * 作用：当前登录态为空时，应该回退到 token 反查用户 ID。
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
     * 作用：Bearer Token 也应被识别并正确反查用户。
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
}
