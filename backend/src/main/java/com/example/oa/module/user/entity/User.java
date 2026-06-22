package com.example.oa.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    private String username;
    private String password;
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
    private LocalDateTime lastLoginTime;
}
