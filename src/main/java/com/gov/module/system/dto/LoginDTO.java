package com.gov.module.system.dto;

import com.gov.common.validation.ValidationPatterns;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = ValidationPatterns.USERNAME, message = "用户名格式不正确")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度应为6到64位")
    private String password;
}
