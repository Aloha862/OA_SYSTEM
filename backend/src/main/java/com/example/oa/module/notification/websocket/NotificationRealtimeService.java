package com.example.oa.module.notification.websocket;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRealtimeService {
    public static final String CHANNEL = "oa:notification:realtime";
    private final String instanceId = UUID.randomUUID().toString();
    private final NotificationWebSocketHandler webSocketHandler;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public boolean publish(Long receiverId, NotificationMessage message) {
        boolean local = webSocketHandler.sendToUser(receiverId, message);
        try {
            stringRedisTemplate.convertAndSend(CHANNEL,
                    objectMapper.writeValueAsString(new RealtimeEnvelope(instanceId, receiverId, message)));
        } catch (Exception e) {
            log.warn("通知Redis Pub/Sub发布失败: receiverId={}, eventId={}", receiverId, message.getEventId(), e);
        }
        return local;
    }

    public void receive(String json) {
        try {
            RealtimeEnvelope envelope = objectMapper.readValue(json, RealtimeEnvelope.class);
            if (!instanceId.equals(envelope.sourceInstanceId())) {
                webSocketHandler.sendToUser(envelope.receiverId(), envelope.message());
            }
        } catch (Exception e) {
            log.warn("通知Redis Pub/Sub消息解析失败", e);
        }
    }

    public record RealtimeEnvelope(String sourceInstanceId, Long receiverId, NotificationMessage message) {
    }
}
