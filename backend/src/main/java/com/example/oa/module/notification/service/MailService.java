package com.example.oa.module.notification.service;

import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.notification.entity.MailOutbox;

public interface MailService {

    void sendNotificationMail(NotificationMessage message);
    PageResult<MailOutbox> pageOutbox(String status, long current, long size);
    void retryOutbox(Long id);
}
