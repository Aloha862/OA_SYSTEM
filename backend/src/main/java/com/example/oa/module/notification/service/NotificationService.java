package com.example.oa.module.notification.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.dto.NotificationQueryRequest;
import com.example.oa.module.notification.dto.SystemNotificationRequest;
import com.example.oa.module.notification.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {

    PageResult<Notification> pageMine(NotificationQueryRequest request);

    long unreadCount();

    void markRead(Long id);

    void markReadBatch(List<Long> ids);

    void deleteMine(Long id);

    void sendSystem(SystemNotificationRequest request);

    Notification createFromMessage(NotificationMessage message);
}
