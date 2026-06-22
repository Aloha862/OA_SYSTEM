package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "消息内容不能超过8000字符")
    private String content;

    @NotBlank(message = "clientMessageId不能为空")
    @Size(max = 64, message = "clientMessageId格式错误")
    private String clientMessageId;
}
