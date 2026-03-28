package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户角色关系实体。
 * 对应 `sys_user_role` 中间表，用来描述用户和角色的多对多关系。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {
    /** 关系主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 ID。 */
    private Long userId;
    /** 角色 ID。 */
    private Long roleId;
}
