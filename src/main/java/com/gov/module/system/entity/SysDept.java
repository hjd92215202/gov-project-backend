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
 * 部门实体。
 * 它既是组织树节点，也是数据权限和审批逐级上报的基础数据。
 */
@Data
@TableName("sys_dept")
public class SysDept {
    /** 部门主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 上级部门 ID，用于构建树结构。 */
    private Long parentId;

    /** 部门名称。 */
    private String deptName;

    /** 部门负责人用户 ID。 */
    private Long leaderId;

    /** 逻辑删除标记。 */
    @TableLogic
    private Integer deleted;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 关联查询出的负责人姓名，仅用于展示。 */
    @TableField(exist = false)
    private String leaderName;
}
