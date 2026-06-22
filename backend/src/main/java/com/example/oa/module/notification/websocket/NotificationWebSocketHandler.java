package com.example.oa.module.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("token invalid"));
            return;
        }
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null && sessions.containsKey(userId)) {
            sessions.get(userId).remove(session);
        }
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
}
