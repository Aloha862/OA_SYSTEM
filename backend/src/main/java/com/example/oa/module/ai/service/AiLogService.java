package com.example.oa.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.ai.dto.AiLogQueryRequest;
import com.example.oa.module.ai.entity.AiLog;

public interface AiLogService extends IService<AiLog> {

    PageResult<AiLog> pageLogs(AiLogQueryRequest request);
}
