package com.gov.module.system.dto;

import lombok.Data;

/**
 * 部门新增请求 DTO。
 */
@Data
public class DeptCreateDTO {
    private Long parentId;
    private String deptName;
    private Long leaderId;
}
