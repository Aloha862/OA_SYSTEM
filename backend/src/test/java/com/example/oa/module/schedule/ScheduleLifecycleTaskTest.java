package com.example.oa.module.schedule;

import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.module.schedule.mapper.ScheduleMapper;
import com.example.oa.module.schedule.mapper.ScheduleParticipantMapper;
import com.example.oa.module.schedule.service.impl.ScheduleServiceImpl;
import com.example.oa.module.schedule.task.ScheduleReminderTask;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleLifecycleTaskTest {

    @Test
    void finishesExpiredSchedulesWithOneConditionalBatchUpdate() {
        ScheduleMapper mapper = mock(ScheduleMapper.class);
        ScheduleServiceImpl service = new ScheduleServiceImpl(
                mock(ScheduleParticipantMapper.class), mock(NotificationProducer.class), mock(AiService.class));
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.finishExpired(any())).thenReturn(3);

        int updated = service.finishExpiredSchedules();

        assertThat(updated).isEqualTo(3);
        verify(mapper).finishExpired(any());
    }

    @Test
    void scheduledEntryDelegatesToScheduleService() {
        var service = mock(com.example.oa.module.schedule.service.ScheduleService.class);
        when(service.finishExpiredSchedules()).thenReturn(2);

        new ScheduleReminderTask(service).finishExpiredSchedules();

        verify(service).finishExpiredSchedules();
    }
}
