package com.example.oa.module.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_schedule_participant")
public class ScheduleParticipant extends BaseEntity {

    private Long scheduleId;
    private Long userId;
    private String status;
}
