package com.example.oa.config;

import com.example.oa.module.notification.websocket.NotificationRealtimeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class RedisPubSubConfig {
    @Bean
    public RedisMessageListenerContainer notificationRedisListener(
            RedisConnectionFactory connectionFactory,
            NotificationRealtimeService realtimeService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) ->
                        realtimeService.receive(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(NotificationRealtimeService.CHANNEL));
        return container;
    }
}
