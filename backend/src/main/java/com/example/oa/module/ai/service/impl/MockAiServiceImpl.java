package com.example.oa.module.ai.service.impl;

import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.dto.AiQaRequest;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.ai.entity.AiLog;
import com.example.oa.module.ai.enums.AiFunctionTypeEnum;
import com.example.oa.module.ai.mapper.AiLogMapper;
import com.example.oa.module.ai.util.AiLogSanitizer;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.approval.entity.Approval;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oa.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiServiceImpl implements AiService {

    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;

    @Override
    public AiResponse approvalSummary(Approval approval) {
        return execute(AiFunctionTypeEnum.APPROVAL_SUMMARY.name(), "审批摘要", approval, () -> {
            String content = "审批[" + approval.getTitle() + "]类型为" + approval.getType()
                    + "，理由：" + nullSafe(approval.getReason())
                    + "，金额：" + nullSafe(approval.getAmount())
                    + "，时间范围：" + nullSafe(approval.getStartTime()) + "至" + nullSafe(approval.getEndTime()) + "。";
            return AiResponse.of(AiFunctionTypeEnum.APPROVAL_SUMMARY.name(), "mock", content, Map.of("summary", content), 0);
        });
    }

    @Override
    public AiResponse approvalRiskAnalyze(Approval approval) {
        return execute(AiFunctionTypeEnum.APPROVAL_RISK.name(), "审批风险分析", approval, () -> {
            String level = "LOW";
            String reason = "未发现明显风险";
            if (approval.getAmount() != null && approval.getAmount().compareTo(new BigDecimal("10000")) > 0) {
                level = "HIGH";
                reason = "报销金额较高，建议核验票据和预算";
            } else if (approval.getReason() == null || approval.getReason().length() < 10) {
                level = "MEDIUM";
                reason = "申请理由偏短，建议补充说明";
            } else if (approval.getStartTime() != null && approval.getEndTime() != null
                    && Duration.between(approval.getStartTime(), approval.getEndTime()).toDays() > 5) {
                level = "MEDIUM";
                reason = "时间跨度较长，建议确认安排合理性";
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("riskLevel", level);
            data.put("reason", reason);
            data.put("suggestion", "请审批人结合部门制度和附件材料人工复核。");
            return AiResponse.of(AiFunctionTypeEnum.APPROVAL_RISK.name(), "mock", reason, data, 0);
        });
    }

    @Override
    public AiResponse generateNews(NewsGenerateRequest request) {
        return execute(AiFunctionTypeEnum.NEWS_GENERATE.name(), "新闻生成", request, () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", request.getTopic() == null ? "企业动态" : request.getTopic());
            data.put("summary", "围绕" + nullSafe(request.getKeywords()) + "整理的一版新闻摘要。");
            data.put("content", "【Mock】本稿围绕“" + nullSafe(request.getTopic()) + "”展开，结合关键词“"
                    + nullSafe(request.getKeywords()) + "”，以" + nullSafe(request.getTone()) + "语气生成，可继续人工编辑后发布。");
            data.put("category", request.getCategory());
            return AiResponse.of(AiFunctionTypeEnum.NEWS_GENERATE.name(), "mock", "新闻草稿已生成", data, 0);
        });
    }

    @Override
    public AiResponse polishNews(NewsPolishRequest request) {
        return execute(AiFunctionTypeEnum.NEWS_POLISH.name(), "新闻润色", request, () -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", "润色版：" + nullSafe(request.getTitle()));
            data.put("summary", "已按" + nullSafe(request.getStyle()) + "风格提炼摘要。");
            data.put("content", "【Mock润色】" + nullSafe(request.getContent()));
            return AiResponse.of(AiFunctionTypeEnum.NEWS_POLISH.name(), "mock", "新闻已润色", data, 0);
        });
    }

    @Override
    public AiResponse parseSchedule(ScheduleParseRequest request) {
        return execute(AiFunctionTypeEnum.SCHEDULE_PARSE.name(), "日程解析", request, () -> {
            LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", "项目会议");
            data.put("content", request.getText());
            data.put("type", "MEETING");
            data.put("startTime", start);
            data.put("endTime", start.plusHours(1));
            data.put("location", request.getText() != null && request.getText().contains("三号会议室") ? "三号会议室" : "待确认");
            data.put("reminderMinutes", request.getText() != null && request.getText().contains("15") ? 15 : 30);
            return AiResponse.of(AiFunctionTypeEnum.SCHEDULE_PARSE.name(), "mock", "日程已解析", data, 0);
        });
    }

    @Override
    public AiResponse qa(AiQaRequest request) {
        return execute(AiFunctionTypeEnum.QA.name(), "智能问答", request, () -> {
            String question = nullSafe(request.getQuestion());
            String answer;
            if (question.contains("请假")) {
                answer = "请假审批请在审批模块选择 LEAVE 类型，填写时间范围和理由后提交。";
            } else if (question.contains("报销")) {
                answer = "报销审批请选择 REIMBURSEMENT 类型，填写金额、事由，并上传票据附件。";
            } else if (question.contains("日程")) {
                answer = "日程可以创建个人日程或会议日程，并设置提前提醒分钟数。";
            } else {
                answer = "这是 OA 助手的 mock 回答：可回答审批流程、请假、报销、加班、日程和新闻查看相关问题。";
            }
            return AiResponse.of(AiFunctionTypeEnum.QA.name(), "mock", answer, Map.of("answer", answer), 0);
        });
    }

    private AiResponse execute(String functionType, String prompt, Object request, Supplier<AiResponse> supplier) {
        long start = System.currentTimeMillis();
        AiLog log = new AiLog();
        log.setUserId(SecurityUtils.currentUserId() == null ? 0L : SecurityUtils.currentUserId());
        log.setFunctionType(functionType);
        log.setProvider("mock");
        log.setModelName("mock-rules");
        log.setPrompt(prompt);
        try {
            log.setRequestContent(AiLogSanitizer.request(objectMapper.writeValueAsString(request)));
            AiResponse response = supplier.get();
            response.setCostTimeMs(System.currentTimeMillis() - start);
            log.setResponseContent(AiLogSanitizer.response(objectMapper.writeValueAsString(response)));
            log.setSuccess(1);
            log.setCostTimeMs(response.getCostTimeMs());
            aiLogMapper.insert(log);
            return response;
        } catch (Exception e) {
            log.setSuccess(0);
            log.setErrorMessage(AiLogSanitizer.error(e.getMessage()));
            log.setCostTimeMs(System.currentTimeMillis() - start);
            aiLogMapper.insert(log);
            throw new RuntimeException("AI mock调用失败", e);
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
