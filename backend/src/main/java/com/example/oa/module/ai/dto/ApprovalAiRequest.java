package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApprovalAiRequest {

    @NotNull(message = "approvalId is required")
    private Long approvalId;
}
