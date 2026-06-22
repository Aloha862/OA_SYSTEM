package com.example.oa.module.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {

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
