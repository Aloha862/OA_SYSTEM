package com.example.oa.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    private String realName;

    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private Integer status;
    private Long departmentId;
    private String position;
    private LocalDate hireDate;
    private Integer isApprover;
}
