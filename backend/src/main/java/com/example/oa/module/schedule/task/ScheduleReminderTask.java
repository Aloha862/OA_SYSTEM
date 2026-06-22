package com.example.oa.module.schedule.task;

import com.example.oa.module.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleReminderTask {

    private final ScheduleService scheduleService;

    @Scheduled(cron = "0 * * * * ?")
    public void scanReminders() {
        try {
            scheduleService.scanAndSendReminders();
        } catch (Exception e) {
            log.error("扫描日程提醒失败", e);
        }
    }
}
