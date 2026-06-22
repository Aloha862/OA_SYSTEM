package com.example.oa.module.approval.controller;

import com.example.oa.common.dto.IdListRequest;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.approval.dto.ApprovalAuditRequest;
import com.example.oa.module.approval.dto.ApprovalQueryRequest;
import com.example.oa.module.approval.dto.ApprovalRequest;
import com.example.oa.module.approval.entity.Approval;
import com.example.oa.module.approval.entity.ApprovalRecord;
import com.example.oa.module.approval.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/page")
    public Result<PageResult<Approval>> page(ApprovalQueryRequest request) {
        return Result.success(approvalService.pageApprovals(request));
    }

    @GetMapping("/my")
    public Result<PageResult<Approval>> my(ApprovalQueryRequest request) {
        return Result.success(approvalService.myApprovals(request));
    }

    @GetMapping("/todo")
    public Result<PageResult<Approval>> todo(ApprovalQueryRequest request) {
        return Result.success(approvalService.todoApprovals(request));
    }

    @GetMapping("/done")
    public Result<PageResult<Approval>> done(ApprovalQueryRequest request) {
        return Result.success(approvalService.doneApprovals(request));
    }

    @GetMapping("/{id}")
    public Result<Approval> detail(@PathVariable Long id) {
        return Result.success(approvalService.detail(id));
    }

    @PostMapping
    public Result<Approval> create(@Valid @RequestBody ApprovalRequest request) {
        return Result.success(approvalService.createApproval(request));
    }

    @PutMapping("/{id}")
    public Result<Approval> update(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        return Result.success(approvalService.updateApproval(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        approvalService.deleteApproval(id);
        return Result.success(null);
    }

    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@Valid @RequestBody IdListRequest request) {
        approvalService.deleteApprovals(request.getIds());
        return Result.success(null);
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        approvalService.submit(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        approvalService.withdraw(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ApprovalAuditRequest request) {
        approvalService.approve(id, request);
        return Result.success(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ApprovalAuditRequest request) {
        approvalService.reject(id, request);
        return Result.success(null);
    }

    @GetMapping("/{id}/records")
    public Result<List<ApprovalRecord>> records(@PathVariable Long id) {
        return Result.success(approvalService.records(id));
    }

    @PostMapping("/{id}/ai-summary")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<AiResponse> aiSummary(@PathVariable Long id) {
        return Result.success(approvalService.aiSummary(id));
    }

    @PostMapping("/{id}/ai-risk")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<AiResponse> aiRisk(@PathVariable Long id) {
        return Result.success(approvalService.aiRisk(id));
    }
}
