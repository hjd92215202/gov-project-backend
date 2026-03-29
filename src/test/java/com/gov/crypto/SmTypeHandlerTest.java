package com.gov.crypto;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 职责：验证 SM4 字段处理器的可配置加解密行为。
 * 为什么存在：国密配置与历史明文兼容策略是安全改造的高风险点，需要测试兜底。
 * 关键输入输出：输入为处理器配置、数据库读写值，输出为加密结果与解密行为。
 * 关联链路：用户手机号、项目联系电话的透明加解密。
 */
class SmTypeHandlerTest {

    private final SmTypeHandler handler = new SmTypeHandler();

    @BeforeEach
    void setUp() {
        SmTypeHandler.configure("1234567812345678", true, true);
    }

    /**
     * 作用：验证写库时会加密，不会明文落库。
     */
    @Test
    void setNonNullParameter_shouldEncryptWhenEnabled() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        doNothing().when(ps).setString(eq(1), anyString());

        handler.setNonNullParameter(ps, 1, "13800000000", JdbcType.VARCHAR);

        org.mockito.ArgumentCaptor<String> captor = forClass(String.class);
        verify(ps).setString(eq(1), captor.capture());
        assertNotEquals("13800000000", captor.getValue());
        assertTrue(captor.getValue().length() > "13800000000".length());
    }

    /**
     * 作用：验证开启明文回退时，历史明文数据仍可被正常读取。
     */
    @Test
    void getNullableResult_shouldFallbackToPlaintextWhenConfigured() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn("13800000000");

        String value = handler.getNullableResult(rs, "phone");

        assertEquals("13800000000", value);
    }

    /**
     * 作用：验证关闭明文回退时，非密文数据会抛异常，便于尽快发现脏数据。
     */
    @Test
    void getNullableResult_shouldThrowWhenPlaintextFallbackDisabled() throws Exception {
        SmTypeHandler.configure("1234567812345678", true, false);
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("phone")).thenReturn("13800000000");

        assertThrows(Exception.class, () -> handler.getNullableResult(rs, "phone"));
    }

    /**
     * 作用：验证密钥校验生效，避免错误配置导致静默风险。
     */
    @Test
    void configure_shouldRejectInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> SmTypeHandler.configure("short-key", true, true));
    }

}
