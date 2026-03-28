package com.gov.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树响应 VO。
 * 供前端树形控件直接渲染组织结构。
 */
@Data
public class SysDeptTreeVO {
    private Long id;
    private Long parentId;
    private String deptName;
    private Long leaderId;
    private String leaderName;
    private List<SysDeptTreeVO> children = new ArrayList<>();
}
