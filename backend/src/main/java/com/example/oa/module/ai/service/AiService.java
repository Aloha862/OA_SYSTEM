package com.example.oa.module.ai.service;

import com.example.oa.module.ai.dto.AiQaRequest;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.approval.entity.Approval;

public interface AiService {

    AiResponse approvalSummary(Approval approval);

    AiResponse approvalRiskAnalyze(Approval approval);

    AiResponse generateNews(NewsGenerateRequest request);

    AiResponse polishNews(NewsPolishRequest request);

    AiResponse parseSchedule(ScheduleParseRequest request);

    AiResponse qa(AiQaRequest request);
}
