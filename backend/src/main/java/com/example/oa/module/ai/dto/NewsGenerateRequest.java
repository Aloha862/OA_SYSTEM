package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewsGenerateRequest {

    @NotBlank(message = "topic is required")
    private String topic;
    private String keywords;
    private String tone;

    @Min(value = 100, message = "words must be at least 100")
    @Max(value = 3000, message = "words must be at most 3000")
    private Integer words;

    @Min(value = 100, message = "wordCount must be at least 100")
    @Max(value = 3000, message = "wordCount must be at most 3000")
    private Integer wordCount;
    private String category;

    public Integer getResolvedWordCount() {
        return wordCount != null ? wordCount : words;
    }
}
