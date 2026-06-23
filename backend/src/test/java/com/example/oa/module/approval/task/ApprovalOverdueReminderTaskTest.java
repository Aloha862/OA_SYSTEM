package com.example.oa.module.approval.task;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.oa.module.approval.entity.Approval;
import com.example.oa.module.approval.mapper.ApprovalMapper;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalOverdueReminderTaskTest {

    @Test
    void queuesOneDeterministicReminderPerApprovalAndDay() {
        ApprovalMapper mapper = mock(ApprovalMapper.class);
        NotificationProducer producer = mock(NotificationProducer.class);
        Approval approval = new Approval();
        approval.setId(17L);
        approval.setApproverId(9L);
        approval.setTitle("季度预算审批");
        approval.setSubmittedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(approval));

        ApprovalOverdueReminderTask task = new ApprovalOverdueReminderTask(mapper, producer);
        ReflectionTestUtils.setField(task, "overdueHours", 24L);
        ReflectionTestUtils.setField(task, "batchSize", 200);
        LocalDate eventDate = LocalDate.of(2026, 6, 23);
        LocalDateTime now = eventDate.atTime(9, 0);

        task.sendOverdueRemindersAt(now, eventDate);
        task.sendOverdueRemindersAt(now, eventDate);

        var messageCaptor = org.mockito.ArgumentCaptor.forClass(NotificationMessage.class);
        verify(producer, times(2)).send(eq("oa.notification.approval"), messageCaptor.capture());
        List<NotificationMessage> messages = messageCaptor.getAllValues();
        assertThat(messages).extracting(NotificationMessage::getEventId)
                .containsOnly("approval-overdue:17:2026-06-23");
        assertThat(messages.get(0).getReceiverId()).isEqualTo(9L);
        assertThat(messages.get(0).getBusinessType()).isEqualTo("APPROVAL");
        assertThat(messages.get(0).getBusinessId()).isEqualTo(17L);
        assertThat(messages.get(0).getContent()).contains("24 小时", "季度预算审批");
    }
}
