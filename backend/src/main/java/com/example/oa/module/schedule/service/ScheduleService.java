package com.example.oa.module.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.schedule.dto.ParticipantRequest;
import com.example.oa.module.schedule.dto.ScheduleQueryRequest;
import com.example.oa.module.schedule.dto.ScheduleRequest;
import com.example.oa.module.schedule.entity.Schedule;

import java.util.List;

public interface ScheduleService extends IService<Schedule> {

    PageResult<Schedule> pageSchedules(ScheduleQueryRequest request);

    List<Schedule> calendar(ScheduleQueryRequest request);

    List<Schedule> today();

    List<Schedule> week();

    Schedule detail(Long id);

    Schedule createSchedule(ScheduleRequest request);

    Schedule updateSchedule(Long id, ScheduleRequest request);

    void deleteSchedule(Long id);

    void addParticipants(Long id, ParticipantRequest request);

    void removeParticipant(Long id, Long userId);

    void accept(Long id);

    void reject(Long id);

    AiResponse parse(ScheduleParseRequest request);

    void scanAndSendReminders();

    int finishExpiredSchedules();
}
