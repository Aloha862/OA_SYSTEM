package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewsPolishRequest {

    private String title;

    @NotBlank(message = "content is required")
    private String content;
    private String style;
}
