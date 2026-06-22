package com.example.oa.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproverUpdateRequest {

    @NotNull(message = "审批人身份不能为空")
    private Boolean approver;
}
