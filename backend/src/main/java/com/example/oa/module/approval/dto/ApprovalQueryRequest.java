package com.example.oa.module.approval.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ApprovalQueryRequest extends PageQuery {

    private String keyword;
    private String approvalNo;
    private String title;
    private String type;
    private String status;
    private Long applicantId;
    private Long approverId;
    private Long departmentId;
}
