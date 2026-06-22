package com.example.oa.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.ai.dto.AiLogQueryRequest;
import com.example.oa.module.ai.entity.AiLog;
import com.example.oa.module.ai.mapper.AiLogMapper;
import com.example.oa.module.ai.service.AiLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiLogServiceImpl extends ServiceImpl<AiLogMapper, AiLog> implements AiLogService {

    @Override
    public PageResult<AiLog> pageLogs(AiLogQueryRequest request) {
        Page<AiLog> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<AiLog>()
                        .eq(request.getUserId() != null, AiLog::getUserId, request.getUserId())
                        .eq(StringUtils.hasText(request.getFunctionType()), AiLog::getFunctionType, request.getFunctionType())
                        .eq(StringUtils.hasText(request.getProvider()), AiLog::getProvider, request.getProvider())
                        .orderByDesc(AiLog::getId));
        return PageResult.of(page);
    }
}
