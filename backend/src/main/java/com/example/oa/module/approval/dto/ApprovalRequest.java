package com.example.oa.module.approval.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApprovalRequest {

    @NotBlank(message = "审批标题不能为空")
    private String title;

    @NotBlank(message = "审批类型不能为空")
    private String type;

    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal amount;
    private String destination;
    private String formData;
}
