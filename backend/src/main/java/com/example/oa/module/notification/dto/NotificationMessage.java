package com.example.oa.module.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage implements Serializable {

    private Long receiverId;
    private Long senderId;
    private String title;
    private String content;
    private String type;
    private String businessType;
    private Long businessId;
    private LocalDateTime createdAt;
}
