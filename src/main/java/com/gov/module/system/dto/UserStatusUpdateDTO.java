package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * 用户状态变更请求 DTO。
 */
@Data
public class UserStatusUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @NotNull(message = "用户状态不能为空")
    @Max(value = 1, message = "用户状态值不合法")
    private Integer status;
}
