package com.example.oa.module.notification.mq;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.service.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final NotificationOutboxService outboxService;

    public void send(String routingKey, NotificationMessage message) {
        outboxService.enqueue(routingKey, message);
    }
}
