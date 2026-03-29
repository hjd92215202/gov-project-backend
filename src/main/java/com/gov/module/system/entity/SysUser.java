package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.crypto.SmTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户实体。
 * 对应 `sys_user` 表，承载登录、组织归属、启停状态等用户基础信息。
 * 它是数据库模型，不直接等同于接口层的 DTO 或 VO。
 */
@Data
@TableName(value = "sys_user", autoResultMap = true)
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属部门 ID。 */
    private Long deptId;
    /** 登录用户名。 */
    private String username;
    /** 加密后的密码摘要。 */
    private String password;
    /** 真实姓名。 */
    private String realName;
    /** 联系电话。 */
    @TableField(typeHandler = SmTypeHandler.class)
    private String phone;
    /** 状态：1启用，0停用。 */
    private Integer status;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 关联查询出的部门名称，仅用于展示。 */
    @TableField(exist = false)
    private String deptName;

    /** 关联查询出的角色名称文本，仅用于展示。 */
    @TableField(exist = false)
    private String roleNames;

    /** 页面编辑时回填的角色 ID 列表，不直接落库。 */
    @TableField(exist = false)
    private List<Long> roleIds;
}
