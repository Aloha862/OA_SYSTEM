package com.example.oa.module.notification.service;

import com.example.oa.module.notification.dto.NotificationMessage;

public interface MailService {

    void sendNotificationMail(NotificationMessage message);
}
