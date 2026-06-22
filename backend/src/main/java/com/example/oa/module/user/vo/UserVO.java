package com.example.oa.module.user.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    private Integer status;
    private Long departmentId;
    private String departmentName;
    private String position;
    private LocalDate hireDate;
    private Integer isApprover;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
