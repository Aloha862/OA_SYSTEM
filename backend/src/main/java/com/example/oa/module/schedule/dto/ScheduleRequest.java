package com.example.oa.module.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleRequest {

    @NotBlank(message = "日程标题不能为空")
    private String title;
    private String content;
    private String type;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private String location;
    private Integer reminderMinutes;
    private String aiOriginText;
    private List<Long> participantIds;
}
