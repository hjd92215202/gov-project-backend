package com.gov.crypto;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SmTypeHandler extends BaseTypeHandler<String> {

    private static volatile SymmetricCrypto sm4;
    private static volatile boolean cryptoEnabled = true;
    private static volatile boolean allowPlaintextFallback = true;

    public static synchronized void configure(String key, boolean enabled, boolean plaintextFallback) {
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.length() != 16) {
            throw new IllegalArgumentException("SM4密钥长度必须为16位");
        }
        sm4 = SmUtil.sm4(normalizedKey.getBytes());
        cryptoEnabled = enabled;
        allowPlaintextFallback = plaintextFallback;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (!cryptoEnabled || StrUtil.isBlank(parameter)) {
            ps.setString(i, parameter);
            return;
        }
        ps.setString(i, requireCrypto().encryptHex(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String value) {
        if (!cryptoEnabled || StrUtil.isBlank(value)) {
            return value;
        }
        try {
            return requireCrypto().decryptStr(value);
        } catch (Exception ex) {
            if (allowPlaintextFallback) {
                return value;
            }
            throw ex;
        }
    }

    private SymmetricCrypto requireCrypto() {
        if (sm4 == null) {
            throw new IllegalStateException("SM4 crypto is not configured");
        }
        return sm4;
    }
}
