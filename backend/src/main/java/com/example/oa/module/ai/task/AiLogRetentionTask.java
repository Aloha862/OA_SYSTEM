package com.example.oa.module.ai.task;

import com.example.oa.module.ai.mapper.AiLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiLogRetentionTask {
    private final AiLogMapper aiLogMapper;

    @Value("${oa.ai.log-retention-days:90}")
    private int retentionDays;

    @Scheduled(cron = "${oa.ai.log-retention-cron:0 20 3 * * ?}",
            zone = "${oa.tasks.zone:Asia/Shanghai}")
    public void purgeExpiredLogs() {
        int deleted = aiLogMapper.purgeBefore(LocalDateTime.now().minusDays(Math.max(7, retentionDays)));
        if (deleted > 0) log.info("已清理过期AI日志: count={}", deleted);
    }
}
