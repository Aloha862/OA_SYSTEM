package com.example.oa.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_conversation")
public class AiConversation extends BaseEntity {
    private Long userId;
    private String title;
    private String mode;
    private String modelName;
    private LocalDateTime lastMessageAt;
}
