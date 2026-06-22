package com.example.oa.module.schedule.controller;

import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.schedule.dto.ParticipantRequest;
import com.example.oa.module.schedule.dto.ScheduleQueryRequest;
import com.example.oa.module.schedule.dto.ScheduleRequest;
import com.example.oa.module.schedule.entity.Schedule;
import com.example.oa.module.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/page")
    public Result<PageResult<Schedule>> page(ScheduleQueryRequest request) {
        return Result.success(scheduleService.pageSchedules(request));
    }

    @GetMapping("/calendar")
    public Result<List<Schedule>> calendar(ScheduleQueryRequest request) {
        return Result.success(scheduleService.calendar(request));
    }

    @GetMapping("/today")
    public Result<List<Schedule>> today() {
        return Result.success(scheduleService.today());
    }

    @GetMapping("/week")
    public Result<List<Schedule>> week() {
        return Result.success(scheduleService.week());
    }

    @GetMapping("/{id}")
    public Result<Schedule> detail(@PathVariable Long id) {
        return Result.success(scheduleService.detail(id));
    }

    @PostMapping
    public Result<Schedule> create(@Valid @RequestBody ScheduleRequest request) {
        return Result.success(scheduleService.createSchedule(request));
    }

    @PutMapping("/{id}")
    public Result<Schedule> update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return Result.success(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/participants")
    public Result<Void> addParticipants(@PathVariable Long id, @Valid @RequestBody ParticipantRequest request) {
        scheduleService.addParticipants(id, request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/participants/{userId}")
    public Result<Void> removeParticipant(@PathVariable Long id, @PathVariable Long userId) {
        scheduleService.removeParticipant(id, userId);
        return Result.success(null);
    }

    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        scheduleService.accept(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        scheduleService.reject(id);
        return Result.success(null);
    }

    @PostMapping("/ai-parse")
    public Result<AiResponse> aiParse(@Valid @RequestBody ScheduleParseRequest request) {
        return Result.success(scheduleService.parse(request));
    }
}
