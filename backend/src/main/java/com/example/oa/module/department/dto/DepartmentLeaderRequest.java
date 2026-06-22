package com.example.oa.module.department.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentLeaderRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
