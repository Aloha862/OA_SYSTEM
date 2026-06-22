package com.example.oa.config;

import com.example.oa.module.notification.websocket.NotificationHandshakeInterceptor;
import com.example.oa.module.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationHandshakeInterceptor notificationHandshakeInterceptor;

    @Value("${oa.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notification")
                .addInterceptors(notificationHandshakeInterceptor)
                .setAllowedOrigins(java.util.Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new));
    }
}
