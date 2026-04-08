package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 部门新增请求 DTO。
 */
@Data
public class DeptCreateDTO {

    private Long parentId;

    @Size(min = 2, max = 50, message = "部门名称长度应为2到50个字符")
    private String deptName;

    private Long leaderId;
}
