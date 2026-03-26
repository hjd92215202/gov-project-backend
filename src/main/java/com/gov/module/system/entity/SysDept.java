package com.gov.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long parentId;
    private String deptName;
    private Long leaderId; // 部门负责人的用户ID
}