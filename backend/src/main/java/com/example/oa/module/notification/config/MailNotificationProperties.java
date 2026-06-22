package com.example.oa.module.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oa.mail")
public class MailNotificationProperties {

    private String notificationRecipient;
}
