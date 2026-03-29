package com.gov.crypto;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 国密 SM4 字段处理器。
 * 用在 MyBatis 字段映射层，实现“写库前加密、读库时解密”的透明处理。
 * 当前主要用于手机号这类需要在库内脱敏保存但页面仍要正常展示的字段。
 */
public class SmTypeHandler extends BaseTypeHandler<String> {

    /** 默认 SM4 密钥（16 字节，仅作兜底，建议通过配置覆盖）。 */
    private static final String DEFAULT_KEY = "1234567812345678";

    private static volatile SymmetricCrypto sm4 = SmUtil.sm4(DEFAULT_KEY.getBytes());
    private static volatile boolean cryptoEnabled = true;
    private static volatile boolean allowPlaintextFallback = true;

    /**
     * 由配置层在应用启动时注入国密参数。
     * 为兼容历史明文数据，默认开启解密失败明文回退。
     */
    public static synchronized void configure(String key, boolean enabled, boolean plaintextFallback) {
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.length() != 16) {
            throw new IllegalArgumentException("SM4密钥长度必须为16位");
        }
        sm4 = SmUtil.sm4(normalizedKey.getBytes());
        cryptoEnabled = enabled;
        allowPlaintextFallback = plaintextFallback;
    }

    /** 写入数据库前执行加密。 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (!cryptoEnabled || StrUtil.isBlank(parameter)) {
            ps.setString(i, parameter);
            return;
        }
        ps.setString(i, sm4.encryptHex(parameter));
    }

    /** 按列名读取结果时执行解密。 */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    /** 按列序号读取结果时执行解密。 */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    /** 存储过程读取结果时执行解密。 */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }

    private String decrypt(String value) {
        if (!cryptoEnabled || StrUtil.isBlank(value)) {
            return value;
        }
        try {
            return sm4.decryptStr(value);
        } catch (Exception ex) {
            if (allowPlaintextFallback) {
                return value;
            }
            throw ex;
        }
    }
}
