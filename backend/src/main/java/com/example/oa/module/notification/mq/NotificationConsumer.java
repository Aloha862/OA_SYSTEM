package com.example.oa.module.notification.mq;

import com.example.oa.common.constant.RabbitMqConstants;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.service.MailService;
import com.example.oa.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final MailService mailService;

    @RabbitListener(queues = RabbitMqConstants.NOTIFICATION_QUEUE)
    public void consume(NotificationMessage notificationMessage) {
        try {
            notificationService.createFromMessage(notificationMessage);
            if (notificationMessage.getType() != null
                    && (notificationMessage.getType().startsWith("schedule.") || notificationMessage.getType().equals("system.notice"))) {
                mailService.sendNotificationMail(notificationMessage);
            }
        } catch (Exception e) {
            log.error("通知消息消费失败: {}", notificationMessage, e);
            throw new IllegalStateException("通知消息消费失败", e);
        }
    }
}
