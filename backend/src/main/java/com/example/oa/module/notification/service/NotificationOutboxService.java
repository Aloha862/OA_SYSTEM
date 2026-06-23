package com.example.oa.module.notification.service;

import com.example.oa.common.result.PageResult;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.entity.NotificationOutbox;

public interface NotificationOutboxService {
    void enqueue(String routingKey, NotificationMessage message);
    PageResult<NotificationOutbox> page(String status, long current, long size);
    void retry(Long id);
}
