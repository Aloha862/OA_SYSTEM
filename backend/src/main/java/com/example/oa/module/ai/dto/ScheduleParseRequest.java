package com.example.oa.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduleParseRequest {

    @NotBlank(message = "text is required")
    private String text;
}
