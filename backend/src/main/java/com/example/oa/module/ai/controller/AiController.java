package com.example.oa.module.ai.controller;

import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.ai.dto.ApprovalAiRequest;
import com.example.oa.module.ai.dto.AiLogQueryRequest;
import com.example.oa.module.ai.dto.AiQaRequest;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.ai.entity.AiLog;
import com.example.oa.module.ai.service.AiLogService;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.approval.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiLogService aiLogService;
    private final ApprovalService approvalService;

    @PostMapping("/approval-summary")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<AiResponse> approvalSummary(@Valid @RequestBody ApprovalAiRequest request) {
        if (request.getApprovalId() == null) {
            throw new BusinessException("请选择审批 ID");
        }
        return Result.success(approvalService.aiSummary(request.getApprovalId()));
    }

    @PostMapping("/approval-risk")
    @PreAuthorize("@securityPermission.isApprover()")
    public Result<AiResponse> approvalRisk(@Valid @RequestBody ApprovalAiRequest request) {
        if (request.getApprovalId() == null) {
            throw new BusinessException("请选择审批 ID");
        }
        return Result.success(approvalService.aiRisk(request.getApprovalId()));
    }

    @PostMapping("/news-generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AiResponse> newsGenerate(@Valid @RequestBody NewsGenerateRequest request) {
        return Result.success(aiService.generateNews(request));
    }

    @PostMapping("/news-polish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AiResponse> newsPolish(@Valid @RequestBody NewsPolishRequest request) {
        return Result.success(aiService.polishNews(request));
    }

    @PostMapping("/schedule-parse")
    public Result<AiResponse> scheduleParse(@Valid @RequestBody ScheduleParseRequest request) {
        return Result.success(aiService.parseSchedule(request));
    }

    @PostMapping("/qa")
    public Result<AiResponse> qa(@Valid @RequestBody AiQaRequest request) {
        return Result.success(aiService.qa(request));
    }

    @GetMapping("/logs/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<AiLog>> logs(AiLogQueryRequest request) {
        return Result.success(aiLogService.pageLogs(request));
    }
}
