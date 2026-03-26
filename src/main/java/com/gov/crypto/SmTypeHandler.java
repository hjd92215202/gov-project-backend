package com.gov.crypto;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis 字段国密 SM4 透明加解密处理器
 * 秘钥建议从配置文件读取，这里演示固定一个 16 位秘钥
 */
public class SmTypeHandler extends BaseTypeHandler<String> {

    private static final String KEY = "1234567812345678"; // 必须是16位
    private static final SymmetricCrypto sm4 = SmUtil.sm4(KEY.getBytes());

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 存入数据库前加密
        ps.setString(i, sm4.encryptHex(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        return columnValue == null ? null : sm4.decryptStr(columnValue);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        return columnValue == null ? null : sm4.decryptStr(columnValue);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        return columnValue == null ? null : sm4.decryptStr(columnValue);
    }
}