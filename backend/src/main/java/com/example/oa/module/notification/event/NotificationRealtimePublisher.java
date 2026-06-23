package com.example.oa.module.notification.event;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oa.module.notification.entity.Notification;
import com.example.oa.module.notification.mapper.NotificationMapper;
import com.example.oa.module.notification.websocket.NotificationRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimePublisher {
    private final NotificationRealtimeService realtimeService;
    private final NotificationMapper notificationMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(NotificationCreatedEvent event) {
        try {
            boolean pushed = realtimeService.publish(event.receiverId(), event.message());
            if (pushed) {
                notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getId, event.notificationId())
                        .set(Notification::getPushed, 1));
            }
        } catch (Exception e) {
            log.warn("通知实时推送失败，用户仍可从通知列表读取: notificationId={}, receiverId={}",
                    event.notificationId(), event.receiverId(), e);
        }
    }
}
