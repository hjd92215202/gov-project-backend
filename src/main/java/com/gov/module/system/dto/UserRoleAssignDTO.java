package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 用户角色分配请求 DTO。
 */
@Data
public class UserRoleAssignDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Size(max = 20, message = "角色数量不能超过20个")
    private List<Long> roleIds;
}
