package com.example.oa.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_message")
public class AiMessage extends BaseEntity {
    private Long conversationId;
    private Long userId;
    private String role;
    private String clientMessageId;
    private Long parentMessageId;
    private String content;
    private String status;
    private String modelName;
    private Long costTimeMs;
    private String errorCode;
    private String errorMessage;
}
