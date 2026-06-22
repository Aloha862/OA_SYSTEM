package com.example.oa.module.approval.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.approval.dto.ApprovalAuditRequest;
import com.example.oa.module.approval.dto.ApprovalQueryRequest;
import com.example.oa.module.approval.dto.ApprovalRequest;
import com.example.oa.module.approval.entity.Approval;
import com.example.oa.module.approval.entity.ApprovalRecord;

import java.util.List;

public interface ApprovalService extends IService<Approval> {

    PageResult<Approval> pageApprovals(ApprovalQueryRequest request);

    PageResult<Approval> myApprovals(ApprovalQueryRequest request);

    PageResult<Approval> todoApprovals(ApprovalQueryRequest request);

    PageResult<Approval> doneApprovals(ApprovalQueryRequest request);

    Approval detail(Long id);

    Approval createApproval(ApprovalRequest request);

    Approval updateApproval(Long id, ApprovalRequest request);

    void deleteApproval(Long id);

    void deleteApprovals(List<Long> ids);

    void submit(Long id);

    void withdraw(Long id);

    void approve(Long id, ApprovalAuditRequest request);

    void reject(Long id, ApprovalAuditRequest request);

    List<ApprovalRecord> records(Long id);

    AiResponse aiSummary(Long id);

    AiResponse aiRisk(Long id);
}
