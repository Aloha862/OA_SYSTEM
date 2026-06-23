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

    @Scheduled(cron = "${oa.tasks.schedule-reminder.cron:0 * * * * ?}",
            zone = "${oa.tasks.zone:Asia/Shanghai}")
    public void scanReminders() {
        try {
            scheduleService.scanAndSendReminders();
        } catch (Exception e) {
            log.error("扫描日程提醒失败", e);
        }
    }

    @Scheduled(cron = "${oa.tasks.schedule-lifecycle.cron:0 */5 * * * ?}",
            zone = "${oa.tasks.zone:Asia/Shanghai}")
    public void finishExpiredSchedules() {
        try {
            int updated = scheduleService.finishExpiredSchedules();
            if (updated > 0) {
                log.info("已自动完成过期日程: count={}", updated);
            }
        } catch (Exception e) {
            log.error("更新过期日程状态失败", e);
        }
    }
}
