package com.example.oa.module.notification.event;

import com.example.oa.module.notification.dto.NotificationMessage;

public record NotificationCreatedEvent(Long notificationId, Long receiverId, NotificationMessage message) {
}
