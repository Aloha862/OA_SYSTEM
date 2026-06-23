package com.example.oa.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_mail_outbox")
public class MailOutbox extends BaseEntity {
    private String eventId;
    private Long receiverId;
    private String recipient;
    private String subject;
    private String htmlContent;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime sentAt;
}
