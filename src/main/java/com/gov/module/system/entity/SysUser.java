package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID) // 雪花算法生成ID
    private Long id;

    private Long deptId;      // 部门ID
    private String username;  // 用户名
    private String password;  // 密码密文
    private String realName;  // 真实姓名
    private String phone;     // 手机号
    private Integer status;   // 状态 1:正常, 0:停用

    @TableLogic
    private Integer deleted;  // 逻辑删除标志

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}