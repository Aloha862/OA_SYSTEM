package com.example.oa.module.user.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryRequest extends PageQuery {

    private String keyword;
    private String username;
    private String realName;
    private String role;
    private Integer status;
    private Long departmentId;
    private Integer isApprover;
}
