package com.example.oa.module.department.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "部门名称不能为空")
    private String name;
    private Long parentId;
    private Long leaderId;
    private Long approverId;
    private Integer sortOrder;
    private Integer status;
}
