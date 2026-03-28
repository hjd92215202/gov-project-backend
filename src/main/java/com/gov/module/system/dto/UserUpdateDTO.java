package com.gov.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户更新请求 DTO。
 * 和新增 DTO 分开，用来明确“更新必须带 id，用户名不可被当作新增字段处理”。
 */
@Data
public class UserUpdateDTO {
    private Long id;
    private String realName;
    private Long deptId;
    private String phone;
    private Integer status;
    private String password;
    private List<Long> roleIds;
}
