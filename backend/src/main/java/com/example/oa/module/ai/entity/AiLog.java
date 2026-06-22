package com.example.oa.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_log")
public class AiLog extends BaseEntity {

    private Long userId;
    private String functionType;
    private String provider;
    private String modelName;
    private String prompt;
    private String requestContent;
    private String responseContent;
    private Integer success;
    private String errorMessage;
    private Long costTimeMs;
}
