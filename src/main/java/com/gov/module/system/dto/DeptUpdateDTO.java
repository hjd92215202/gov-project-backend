package com.gov.module.system.dto;

import lombok.Data;

/**
 * 部门更新请求 DTO。
 */
@Data
public class DeptUpdateDTO {
    private Long id;
    private Long parentId;
    private String deptName;
    private Long leaderId;
}
