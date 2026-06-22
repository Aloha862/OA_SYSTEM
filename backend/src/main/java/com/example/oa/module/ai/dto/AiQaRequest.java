package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiQaRequest {

    @NotBlank(message = "question is required")
    private String question;
}
