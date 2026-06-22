package com.example.oa.module.notification;

import com.example.oa.module.notification.dto.NotificationMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageTest {
    @Test
    void eachBusinessEventGetsAnIdempotencyId() {
        var first = message();
        var second = message();
        assertThat(first.getEventId()).isNotBlank();
        assertThat(second.getEventId()).isNotEqualTo(first.getEventId());
    }

    private NotificationMessage message() {
        return new NotificationMessage(1L, 2L, "测试", "正文", "system.notice", "SYSTEM", null, LocalDateTime.now());
    }
}
