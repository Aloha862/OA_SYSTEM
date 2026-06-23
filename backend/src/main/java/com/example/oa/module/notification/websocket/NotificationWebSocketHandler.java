package com.example.oa.module.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token invalid"));
            return;
        }
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, 10_000, 512 * 1024);
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(safeSession);
        lastSeen.put(session.getId(), System.currentTimeMillis());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (message.getPayload().contains("PONG")) {
            lastSeen.put(session.getId(), System.currentTimeMillis());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null && sessions.containsKey(userId)) {
            sessions.get(userId).removeIf(item -> item.getId().equals(session.getId()));
            if (sessions.get(userId).isEmpty()) sessions.remove(userId);
        }
        lastSeen.remove(session.getId());
    }

    public boolean sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return false;
        }
        boolean sent = false;
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("WebSocket消息序列化失败", e);
            return false;
        }
        for (WebSocketSession session : userSessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(body));
                    sent = true;
                }
            } catch (Exception e) {
                log.warn("WebSocket推送失败: userId={}", userId, e);
            }
        }
        return sent;
    }

    @Scheduled(fixedDelayString = "${oa.websocket.heartbeat-ms:25000}")
    public void heartbeat() {
        long now = System.currentTimeMillis();
        sessions.forEach((userId, userSessions) -> userSessions.removeIf(session -> {
            try {
                if (!session.isOpen()) return true;
                long seen = lastSeen.getOrDefault(session.getId(), now);
                if (now - seen > 70_000) {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("heartbeat timeout"));
                    lastSeen.remove(session.getId());
                    return true;
                }
                session.sendMessage(new TextMessage("{\"type\":\"PING\"}"));
                return false;
            } catch (Exception e) {
                log.warn("WebSocket心跳失败: userId={}", userId, e);
                lastSeen.remove(session.getId());
                return true;
            }
        }));
    }
}
