package com.gov.module.system.dto;

import com.gov.common.validation.ValidationPatterns;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 用户更新请求 DTO。
 */
@Data
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Pattern(regexp = "^$|" + ValidationPatterns.DISPLAY_NAME, message = "姓名格式不正确")
    private String realName;

    private Long deptId;

    @Pattern(regexp = "^$|" + ValidationPatterns.PHONE, message = "手机号格式不正确")
    private String phone;

    @Max(value = 1, message = "用户状态值不合法")
    private Integer status;

    @Size(min = 6, max = 64, message = "密码长度应为6到64位")
    private String password;

    @Size(max = 20, message = "角色数量不能超过20个")
    private List<Long> roleIds;
}
