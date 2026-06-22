package com.example.oa.module.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_schedule")
public class Schedule extends BaseEntity {

    private String title;
    private String content;
    private String type;
    private Long creatorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private Integer reminderMinutes;
    private LocalDateTime reminderTime;
    private Integer reminderStatus;
    private String status;
    private String aiOriginText;
}
