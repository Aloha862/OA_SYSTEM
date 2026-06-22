package com.example.oa.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {

    private String functionType;
    private String provider;
    private String content;
    private Object data;
    private Long costTimeMs;

    public static AiResponse of(String functionType, String provider, String content, Object data, long costTimeMs) {
        return new AiResponse(functionType, provider, content, data, costTimeMs);
    }
}
