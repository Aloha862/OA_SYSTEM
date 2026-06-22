package com.example.oa.module.user.dto;

import lombok.Data;

@Data
public class UserProfileRequest {

    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private String position;
}
