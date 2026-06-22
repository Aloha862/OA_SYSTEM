package com.example.oa.module.schedule.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleQueryRequest extends PageQuery {

    private String keyword;
    private String title;
    private String type;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
