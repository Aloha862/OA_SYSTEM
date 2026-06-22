package com.example.oa.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class Notification extends BaseEntity {

    private String eventId;
    private Long receiverId;
    private Long senderId;
    private String title;
    private String content;
    private String type;
    private String businessType;
    private Long businessId;
    private Integer readStatus;
    private LocalDateTime readTime;
    private Integer pushed;
}
