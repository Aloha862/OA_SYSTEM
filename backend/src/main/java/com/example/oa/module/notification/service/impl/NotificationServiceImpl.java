package com.example.oa.module.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.cache.CacheSupport;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.dto.NotificationQueryRequest;
import com.example.oa.module.notification.dto.SystemNotificationRequest;
import com.example.oa.module.notification.entity.Notification;
import com.example.oa.module.notification.event.NotificationCreatedEvent;
import com.example.oa.module.notification.mapper.NotificationMapper;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.module.notification.service.NotificationService;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheSupport cacheSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationProducer notificationProducer;
    private final UserMapper userMapper;

    @Override
    public PageResult<Notification> pageMine(NotificationQueryRequest request) {
        Long userId = SecurityUtils.currentUserId();
        String keyword = request.getKeyword();
        Page<Notification> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, userId)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(Notification::getTitle, keyword)
                                .or()
                                .like(Notification::getContent, keyword))
                        .eq(request.getReadStatus() != null, Notification::getReadStatus, request.getReadStatus())
                        .eq(StringUtils.hasText(request.getType()), Notification::getType, request.getType())
                        .orderByDesc(Notification::getId));
        return PageResult.of(page);
    }

    @Override
    public long unreadCount() {
        Long userId = SecurityUtils.currentUserId();
        String key = CacheConstants.NOTIFICATION_UNREAD_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(String.valueOf(cached));
            }
        } catch (Exception e) {
            log.warn("读取未读通知缓存失败: userId={}", userId, e);
        }
        long count = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getReadStatus, 0));
        try {
            redisTemplate.opsForValue().set(key, count, cacheSupport.jitter(CacheConstants.NOTIFICATION_UNREAD_TTL));
        } catch (Exception e) {
            log.warn("写入未读通知缓存失败: userId={}", userId, e);
        }
        return count;
    }

    @Override
    public void markRead(Long id) {
        Notification notification = getRequired(id);
        ensureMine(notification);
        notification.setReadStatus(1);
        notification.setReadTime(LocalDateTime.now());
        updateById(notification);
        clearUnreadCache(notification.getReceiverId());
    }

    @Override
    public void markReadBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        Long userId = SecurityUtils.currentUserId();
        listByIds(ids).forEach(notification -> {
            if (notification.getReceiverId().equals(userId)) {
                notification.setReadStatus(1);
                notification.setReadTime(LocalDateTime.now());
                updateById(notification);
            }
        });
        clearUnreadCache(userId);
    }

    @Override
    public void deleteMine(Long id) {
        Notification notification = getRequired(id);
        ensureMine(notification);
        removeById(id);
        clearUnreadCache(notification.getReceiverId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSystem(SystemNotificationRequest request) {
        List<Long> receivers = request.getReceiverIds();
        if (CollectionUtils.isEmpty(receivers)) {
            receivers = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1))
                    .stream().map(User::getId).toList();
        }
        Long senderId = SecurityUtils.currentUserId();
        for (Long receiverId : receivers) {
            notificationProducer.send("oa.notification.system",
                    new NotificationMessage(receiverId, senderId, request.getTitle(), request.getContent(),
                            "system.notice", "SYSTEM", null, LocalDateTime.now()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createFromMessage(NotificationMessage message) {
        if (!StringUtils.hasText(message.getEventId())) {
            message.setEventId(java.util.UUID.randomUUID().toString());
        }
        Notification existing = getOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getEventId, message.getEventId())
                .eq(Notification::getReceiverId, message.getReceiverId())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Notification notification = BeanUtil.copyProperties(message, Notification.class);
        notification.setReadStatus(0);
        notification.setPushed(0);
        save(notification);
        clearUnreadCache(notification.getReceiverId());
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                notification.getId(), notification.getReceiverId(), message));
        return notification;
    }

    private Notification getRequired(Long id) {
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        return notification;
    }

    private void ensureMine(Notification notification) {
        if (!SecurityUtils.isAdmin() && !notification.getReceiverId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "只能操作自己的通知");
        }
    }

    private void clearUnreadCache(Long userId) {
        try {
            cacheSupport.deleteAfterCommit(CacheConstants.NOTIFICATION_UNREAD_PREFIX + userId);
        } catch (Exception e) {
            log.warn("清理未读通知缓存失败: userId={}", userId, e);
        }
    }
}
