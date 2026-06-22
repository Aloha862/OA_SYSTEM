package com.example.oa.module.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SystemNotificationRequest {

    private List<Long> receiverIds;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;
}
