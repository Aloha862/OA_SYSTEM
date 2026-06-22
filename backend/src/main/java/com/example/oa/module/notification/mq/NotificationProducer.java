package com.example.oa.module.notification.mq;

import com.example.oa.common.constant.RabbitMqConstants;
import com.example.oa.module.notification.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(String routingKey, NotificationMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConstants.NOTIFICATION_EXCHANGE, routingKey, message);
    }
}
