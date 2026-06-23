package com.example.oa.module.approval.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oa.module.approval.entity.Approval;
import com.example.oa.module.approval.enums.ApprovalStatusEnum;
import com.example.oa.module.approval.mapper.ApprovalMapper;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "oa.tasks.approval-overdue", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ApprovalOverdueReminderTask {

    private static final String ROUTING_KEY = "oa.notification.approval";
    private final ApprovalMapper approvalMapper;
    private final NotificationProducer notificationProducer;

    @Value("${oa.tasks.approval-overdue.overdue-hours:24}")
    private long overdueHours;

    @Value("${oa.tasks.approval-overdue.batch-size:200}")
    private int batchSize;

    @Value("${oa.tasks.zone:Asia/Shanghai}")
    private String zone;

    @Scheduled(cron = "${oa.tasks.approval-overdue.cron:0 0 9 * * MON-FRI}",
            zone = "${oa.tasks.zone:Asia/Shanghai}")
    public void sendOverdueReminders() {
        try {
            ZonedDateTime current = ZonedDateTime.now(ZoneId.of(zone));
            sendOverdueRemindersAt(current.toLocalDateTime(), current.toLocalDate());
        } catch (Exception e) {
            log.error("扫描逾期待审批任务失败", e);
        }
    }

    void sendOverdueRemindersAt(LocalDateTime now, LocalDate eventDate) {
        long effectiveHours = Math.max(1, overdueHours);
        int effectiveBatchSize = Math.max(1, Math.min(batchSize, 1000));
        List<Approval> approvals = approvalMapper.selectList(new LambdaQueryWrapper<Approval>()
                .eq(Approval::getStatus, ApprovalStatusEnum.PENDING.name())
                .isNotNull(Approval::getApproverId)
                .isNotNull(Approval::getSubmittedAt)
                .le(Approval::getSubmittedAt, now.minusHours(effectiveHours))
                .orderByAsc(Approval::getSubmittedAt)
                .last("LIMIT " + effectiveBatchSize));

        int submitted = 0;
        for (Approval approval : approvals) {
            try {
                NotificationMessage message = new NotificationMessage();
                message.setEventId("approval-overdue:" + approval.getId() + ":" + eventDate);
                message.setReceiverId(approval.getApproverId());
                message.setSenderId(null);
                message.setTitle("待审批事项逾期提醒");
                message.setContent("审批“" + approval.getTitle() + "”已等待超过 " + effectiveHours + " 小时，请及时处理。");
                message.setType("approval.overdue");
                message.setBusinessType("APPROVAL");
                message.setBusinessId(approval.getId());
                message.setCreatedAt(now);
                notificationProducer.send(ROUTING_KEY, message);
                submitted++;
            } catch (Exception e) {
                log.error("加入逾期审批提醒队列失败: approvalId={}", approval.getId(), e);
            }
        }
        if (submitted > 0) {
            log.info("逾期审批提醒扫描完成: submitted={}, date={}", submitted, eventDate);
        }
    }
}
