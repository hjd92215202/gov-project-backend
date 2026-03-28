package com.gov.module.system.dto;

import lombok.Data;

/**
 * 用户状态变更请求 DTO。
 * 单独拆出来，是为了避免简单启停操作误带入整份用户信息。
 */
@Data
public class UserStatusUpdateDTO {
    private Long id;
    private Integer status;
}
