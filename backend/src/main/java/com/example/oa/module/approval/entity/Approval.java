package com.example.oa.module.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval")
public class Approval extends BaseEntity {

    private String approvalNo;
    private String title;
    private String type;
    private String status;
    private Long applicantId;
    private Long departmentId;
    private Long approverId;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal amount;
    private String destination;
    private String formData;
    private String aiSummary;
    private String aiRiskLevel;
    private String aiRiskSuggestion;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
}
