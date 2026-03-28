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
 * 国密 SM4 字段处理器。
 * 用在 MyBatis 字段映射层，实现“写库前加密、读库时解密”的透明处理。
 * 当前主要用于手机号这类需要在库内脱敏保存但页面仍要正常展示的字段。
 */
public class SmTypeHandler extends BaseTypeHandler<String> {

    /** 演示用固定密钥，生产环境建议从安全配置中读取。 */
    private static final String KEY = "1234567812345678";
    private static final SymmetricCrypto SM4 = SmUtil.sm4(KEY.getBytes());

    /** 写入数据库前执行加密。 */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, SM4.encryptHex(parameter));
    }

    /** 按列名读取结果时执行解密。 */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        return columnValue == null ? null : SM4.decryptStr(columnValue);
    }

    /** 按列序号读取结果时执行解密。 */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        return columnValue == null ? null : SM4.decryptStr(columnValue);
    }

    /** 存储过程读取结果时执行解密。 */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        return columnValue == null ? null : SM4.decryptStr(columnValue);
    }
}
