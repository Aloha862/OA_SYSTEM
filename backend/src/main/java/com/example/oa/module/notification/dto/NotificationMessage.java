package com.example.oa.module.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationMessage implements Serializable {

    private String eventId;
    private Long receiverId;
    private Long senderId;
    private String title;
    private String content;
    private String type;
    private String businessType;
    private Long businessId;
    private LocalDateTime createdAt;

    public NotificationMessage(Long receiverId, Long senderId, String title, String content, String type,
                               String businessType, Long businessId, LocalDateTime createdAt) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.businessType = businessType;
        this.businessId = businessId;
        this.createdAt = createdAt;
    }
}
