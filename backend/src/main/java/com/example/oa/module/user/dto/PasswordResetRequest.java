package com.example.oa.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @NotBlank(message = "新密码不能为空")
    private String password;
}
