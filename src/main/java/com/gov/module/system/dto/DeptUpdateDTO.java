package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 部门更新请求 DTO。
 */
@Data
public class DeptUpdateDTO {

    @NotNull(message = "部门ID不能为空")
    private Long id;

    private Long parentId;

    @Size(min = 2, max = 50, message = "部门名称长度应为2到50个字符")
    private String deptName;

    private Long leaderId;
}
