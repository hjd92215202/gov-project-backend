package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 角色实体。
 * 角色既参与权限判断，也承载菜单权限串，是 RBAC 模型里的核心对象。
 */
@Data
@TableName("sys_role")
public class SysRole {
    /** 角色主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 角色名称。 */
    private String roleName;
    /** 角色编码，会在权限链路中被标准化。 */
    private String roleCode;
    /** 菜单权限串，使用逗号分隔。 */
    private String menuPerms;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
