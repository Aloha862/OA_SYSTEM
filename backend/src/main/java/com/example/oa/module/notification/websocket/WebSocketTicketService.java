package com.example.oa.module.notification.websocket;

import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketTicketService {
    private final RedisTemplate<String, Object> redisTemplate;

    public String issue() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CacheConstants.WS_TICKET_PREFIX + ticket, userId, Duration.ofSeconds(60));
        return ticket;
    }

    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) return null;
        Object value = redisTemplate.opsForValue().getAndDelete(CacheConstants.WS_TICKET_PREFIX + ticket);
        if (value == null) return null;
        return Long.valueOf(String.valueOf(value));
    }
}
