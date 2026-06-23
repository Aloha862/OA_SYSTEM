package com.example.oa.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.oa.common.constant.RabbitMqConstants;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.entity.NotificationOutbox;
import com.example.oa.module.notification.mapper.NotificationOutboxMapper;
import com.example.oa.module.notification.service.NotificationOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl implements NotificationOutboxService {
    private static final int MAX_ATTEMPTS = 10;
    private static final int SENDING_LEASE_MINUTES = 2;
    private final NotificationOutboxMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enqueue(String routingKey, NotificationMessage message) {
        if (!StringUtils.hasText(message.getEventId())) {
            message.setEventId(java.util.UUID.randomUUID().toString());
        }
        NotificationOutbox row = new NotificationOutbox();
        row.setEventId(message.getEventId());
        row.setRoutingKey(routingKey);
        row.setStatus("PENDING");
        row.setRetryCount(0);
        try {
            row.setPayloadJson(objectMapper.writeValueAsString(message));
            mapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            log.info("通知Outbox重复事件已忽略: eventId={}", message.getEventId());
        } catch (Exception e) {
            throw new IllegalStateException("通知Outbox写入失败", e);
        }
    }

    @Override
    public PageResult<NotificationOutbox> page(String status, long current, long size) {
        Page<NotificationOutbox> result = mapper.selectPage(new Page<>(Math.max(current, 1), Math.max(1, Math.min(size, 100))),
                new LambdaQueryWrapper<NotificationOutbox>()
                        .eq(StringUtils.hasText(status), NotificationOutbox::getStatus, status)
                        .orderByDesc(NotificationOutbox::getId));
        return PageResult.of(result);
    }

    @Override
    public void retry(Long id) {
        int updated = mapper.update(null, new LambdaUpdateWrapper<NotificationOutbox>()
                .eq(NotificationOutbox::getId, id)
                .in(NotificationOutbox::getStatus, List.of("FAILED", "FAILED_FINAL"))
                .set(NotificationOutbox::getStatus, "PENDING")
                .set(NotificationOutbox::getRetryCount, 0)
                .set(NotificationOutbox::getNextRetryAt, null)
                .set(NotificationOutbox::getLastError, null)
                .set(NotificationOutbox::getUpdatedAt, LocalDateTime.now()));
        if (updated != 1) {
            throw new BusinessException(409, "仅失败的通知 Outbox 可以重试");
        }
    }

    @Scheduled(fixedDelayString = "${oa.notification.outbox.fixed-delay-ms:3000}")
    public void publishDue() {
        LocalDateTime now = LocalDateTime.now();
        mapper.update(null, new LambdaUpdateWrapper<NotificationOutbox>()
                .eq(NotificationOutbox::getStatus, "SENDING")
                .le(NotificationOutbox::getUpdatedAt, now.minusMinutes(SENDING_LEASE_MINUTES))
                .set(NotificationOutbox::getStatus, "FAILED")
                .set(NotificationOutbox::getNextRetryAt, now)
                .set(NotificationOutbox::getLastError, "发送租约超时，等待重新发布")
                .set(NotificationOutbox::getUpdatedAt, now));
        List<NotificationOutbox> due = mapper.selectList(new LambdaQueryWrapper<NotificationOutbox>()
                .in(NotificationOutbox::getStatus, List.of("PENDING", "FAILED"))
                .and(w -> w.isNull(NotificationOutbox::getNextRetryAt).or().le(NotificationOutbox::getNextRetryAt, now))
                .orderByAsc(NotificationOutbox::getId)
                .last("LIMIT 50"));
        due.forEach(this::publishOne);
    }

    private void publishOne(NotificationOutbox row) {
        boolean claimed = mapper.update(null, new LambdaUpdateWrapper<NotificationOutbox>()
                .eq(NotificationOutbox::getId, row.getId())
                .in(NotificationOutbox::getStatus, List.of("PENDING", "FAILED"))
                .set(NotificationOutbox::getStatus, "SENDING")
                .set(NotificationOutbox::getUpdatedAt, LocalDateTime.now())) == 1;
        if (!claimed) return;
        try {
            NotificationMessage message = objectMapper.readValue(row.getPayloadJson(), NotificationMessage.class);
            CorrelationData correlation = new CorrelationData(row.getEventId());
            rabbitTemplate.convertAndSend(RabbitMqConstants.NOTIFICATION_EXCHANGE, row.getRoutingKey(), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(8, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException(confirm.getReason() == null ? "消息未路由" : confirm.getReason());
            }
            mapper.update(null, new LambdaUpdateWrapper<NotificationOutbox>()
                    .eq(NotificationOutbox::getId, row.getId())
                    .set(NotificationOutbox::getStatus, "SENT")
                    .set(NotificationOutbox::getSentAt, LocalDateTime.now())
                    .set(NotificationOutbox::getLastError, null));
        } catch (Exception e) {
            int retries = row.getRetryCount() == null ? 1 : row.getRetryCount() + 1;
            mapper.update(null, new LambdaUpdateWrapper<NotificationOutbox>()
                    .eq(NotificationOutbox::getId, row.getId())
                    .set(NotificationOutbox::getStatus, retries >= MAX_ATTEMPTS ? "FAILED_FINAL" : "FAILED")
                    .set(NotificationOutbox::getRetryCount, retries)
                    .set(NotificationOutbox::getNextRetryAt, LocalDateTime.now().plusSeconds(backoffSeconds(retries)))
                    .set(NotificationOutbox::getLastError, safeError(e)));
            log.warn("通知Outbox发布失败: eventId={}, retry={}", row.getEventId(), retries, e);
        }
    }

    private long backoffSeconds(int retry) {
        return Math.min(300, 1L << Math.min(retry, 8));
    }

    private String safeError(Exception e) {
        String value = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return value.substring(0, Math.min(500, value.length()));
    }
}
