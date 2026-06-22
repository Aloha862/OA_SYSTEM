package com.example.oa.module.schedule.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.ScheduleParseRequest;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.module.schedule.dto.ParticipantRequest;
import com.example.oa.module.schedule.dto.ScheduleQueryRequest;
import com.example.oa.module.schedule.dto.ScheduleRequest;
import com.example.oa.module.schedule.entity.Schedule;
import com.example.oa.module.schedule.entity.ScheduleParticipant;
import com.example.oa.module.schedule.enums.ScheduleStatusEnum;
import com.example.oa.module.schedule.mapper.ScheduleMapper;
import com.example.oa.module.schedule.mapper.ScheduleParticipantMapper;
import com.example.oa.module.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    private final ScheduleParticipantMapper participantMapper;
    private final NotificationProducer notificationProducer;
    private final AiService aiService;

    @Override
    public PageResult<Schedule> pageSchedules(ScheduleQueryRequest request) {
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), visibleQuery(request)));
    }

    @Override
    public List<Schedule> calendar(ScheduleQueryRequest request) {
        return list(visibleQuery(request));
    }

    @Override
    public List<Schedule> today() {
        LocalDate today = LocalDate.now();
        ScheduleQueryRequest request = new ScheduleQueryRequest();
        request.setStartTime(today.atStartOfDay());
        request.setEndTime(today.plusDays(1).atStartOfDay());
        return calendar(request);
    }

    @Override
    public List<Schedule> week() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        ScheduleQueryRequest request = new ScheduleQueryRequest();
        request.setStartTime(monday.atStartOfDay());
        request.setEndTime(monday.plusDays(7).atStartOfDay());
        return calendar(request);
    }

    @Override
    public Schedule detail(Long id) {
        Schedule schedule = getRequired(id);
        ensureVisible(schedule);
        return schedule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule createSchedule(ScheduleRequest request) {
        Schedule schedule = BeanUtil.copyProperties(request, Schedule.class);
        schedule.setCreatorId(SecurityUtils.currentUserId());
        fillDefaults(schedule);
        save(schedule);
        saveParticipants(schedule.getId(), request.getParticipantIds());
        return schedule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Schedule updateSchedule(Long id, ScheduleRequest request) {
        Schedule schedule = getRequired(id);
        ensureCreatorOrAdmin(schedule);
        BeanUtil.copyProperties(request, schedule, "id", "creatorId", "reminderStatus");
        fillDefaults(schedule);
        updateById(schedule);
        if (request.getParticipantIds() != null) {
            participantMapper.delete(new LambdaQueryWrapper<ScheduleParticipant>().eq(ScheduleParticipant::getScheduleId, id));
            saveParticipants(id, request.getParticipantIds());
        }
        return schedule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        Schedule schedule = getRequired(id);
        ensureCreatorOrAdmin(schedule);
        removeById(id);
        participantMapper.delete(new LambdaQueryWrapper<ScheduleParticipant>().eq(ScheduleParticipant::getScheduleId, id));
    }

    @Override
    public void addParticipants(Long id, ParticipantRequest request) {
        Schedule schedule = getRequired(id);
        ensureCreatorOrAdmin(schedule);
        saveParticipants(id, request.getUserIds());
    }

    @Override
    public void removeParticipant(Long id, Long userId) {
        Schedule schedule = getRequired(id);
        ensureCreatorOrAdmin(schedule);
        participantMapper.delete(new LambdaQueryWrapper<ScheduleParticipant>()
                .eq(ScheduleParticipant::getScheduleId, id)
                .eq(ScheduleParticipant::getUserId, userId));
    }

    @Override
    public void accept(Long id) {
        updateParticipantStatus(id, "ACCEPTED");
    }

    @Override
    public void reject(Long id) {
        updateParticipantStatus(id, "REJECTED");
    }

    @Override
    public AiResponse parse(ScheduleParseRequest request) {
        return aiService.parseSchedule(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanAndSendReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Schedule> schedules = list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getReminderStatus, 0)
                .eq(Schedule::getStatus, ScheduleStatusEnum.NORMAL.name())
                .le(Schedule::getReminderTime, now));
        for (Schedule schedule : schedules) {
            // Conditional claim keeps concurrent schedulers from publishing the same reminder.
            boolean claimed = update(new LambdaUpdateWrapper<Schedule>()
                    .eq(Schedule::getId, schedule.getId())
                    .eq(Schedule::getReminderStatus, 0)
                    .set(Schedule::getReminderStatus, 2));
            if (!claimed) {
                continue;
            }
            try {
            List<Long> receivers = new ArrayList<>();
            receivers.add(schedule.getCreatorId());
            receivers.addAll(participantMapper.selectList(new LambdaQueryWrapper<ScheduleParticipant>()
                            .eq(ScheduleParticipant::getScheduleId, schedule.getId()))
                    .stream().map(ScheduleParticipant::getUserId).toList());
            receivers.stream().distinct().forEach(receiverId -> notificationProducer.send("oa.notification.schedule",
                    new NotificationMessage(receiverId, schedule.getCreatorId(), "日程提醒",
                            schedule.getTitle() + " 即将开始", "schedule.reminder", "SCHEDULE",
                            schedule.getId(), LocalDateTime.now())));
            schedule.setReminderStatus(1);
            updateById(schedule);
            } catch (RuntimeException e) {
                update(new LambdaUpdateWrapper<Schedule>()
                        .eq(Schedule::getId, schedule.getId())
                        .eq(Schedule::getReminderStatus, 2)
                        .set(Schedule::getReminderStatus, 0));
                throw e;
            }
        }
    }

    private LambdaQueryWrapper<Schedule> visibleQuery(ScheduleQueryRequest request) {
        String keyword = request.getKeyword();
        LambdaQueryWrapper<Schedule> query = new LambdaQueryWrapper<Schedule>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Schedule::getTitle, keyword)
                        .or()
                        .like(Schedule::getContent, keyword)
                        .or()
                        .like(Schedule::getLocation, keyword))
                .like(StringUtils.hasText(request.getTitle()), Schedule::getTitle, request.getTitle())
                .eq(StringUtils.hasText(request.getType()), Schedule::getType, request.getType())
                .eq(StringUtils.hasText(request.getStatus()), Schedule::getStatus, request.getStatus())
                .ge(request.getStartTime() != null, Schedule::getStartTime, request.getStartTime())
                .lt(request.getEndTime() != null, Schedule::getStartTime, request.getEndTime())
                .orderByAsc(Schedule::getStartTime);
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.currentUserId();
            List<Long> joined = participantMapper.selectList(new LambdaQueryWrapper<ScheduleParticipant>()
                            .eq(ScheduleParticipant::getUserId, userId))
                    .stream().map(ScheduleParticipant::getScheduleId).distinct().toList();
            query.and(wrapper -> {
                wrapper.eq(Schedule::getCreatorId, userId);
                if (!joined.isEmpty()) {
                    wrapper.or().in(Schedule::getId, joined);
                }
            });
        }
        return query;
    }

    private void fillDefaults(Schedule schedule) {
        if (!StringUtils.hasText(schedule.getType())) {
            schedule.setType("PERSONAL");
        }
        if (!StringUtils.hasText(schedule.getStatus())) {
            schedule.setStatus(ScheduleStatusEnum.NORMAL.name());
        }
        if (schedule.getReminderMinutes() == null) {
            schedule.setReminderMinutes(15);
        }
        schedule.setReminderTime(schedule.getStartTime().minusMinutes(schedule.getReminderMinutes()));
        if (schedule.getReminderStatus() == null) {
            schedule.setReminderStatus(0);
        }
    }

    private void saveParticipants(Long scheduleId, List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        Schedule schedule = getById(scheduleId);
        for (Long userId : userIds.stream().distinct().toList()) {
            if (participantMapper.selectCount(new LambdaQueryWrapper<ScheduleParticipant>()
                    .eq(ScheduleParticipant::getScheduleId, scheduleId)
                    .eq(ScheduleParticipant::getUserId, userId)) > 0) {
                continue;
            }
            ScheduleParticipant participant = new ScheduleParticipant();
            participant.setScheduleId(scheduleId);
            participant.setUserId(userId);
            participant.setStatus("PENDING");
            participantMapper.insert(participant);
            notificationProducer.send("oa.notification.schedule",
                    new NotificationMessage(userId, schedule.getCreatorId(), "会议邀请",
                            schedule.getTitle(), "schedule.invite", "SCHEDULE", scheduleId, LocalDateTime.now()));
        }
    }

    private void updateParticipantStatus(Long scheduleId, String status) {
        ScheduleParticipant participant = participantMapper.selectOne(new LambdaQueryWrapper<ScheduleParticipant>()
                .eq(ScheduleParticipant::getScheduleId, scheduleId)
                .eq(ScheduleParticipant::getUserId, SecurityUtils.currentUserId()));
        if (participant == null) {
            throw new BusinessException("当前用户不是该日程参与人");
        }
        participant.setStatus(status);
        participantMapper.updateById(participant);
    }

    private Schedule getRequired(Long id) {
        Schedule schedule = getById(id);
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }
        return schedule;
    }

    private void ensureVisible(Schedule schedule) {
        Long userId = SecurityUtils.currentUserId();
        boolean participant = participantMapper.selectCount(new LambdaQueryWrapper<ScheduleParticipant>()
                .eq(ScheduleParticipant::getScheduleId, schedule.getId())
                .eq(ScheduleParticipant::getUserId, userId)) > 0;
        if (!SecurityUtils.isAdmin() && !Objects.equals(schedule.getCreatorId(), userId) && !participant) {
            throw new BusinessException(403, "Only related users can view this schedule");
        }
    }

    private void ensureCreatorOrAdmin(Schedule schedule) {
        if (!SecurityUtils.isAdmin() && !Objects.equals(schedule.getCreatorId(), SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "只能操作自己创建的日程");
        }
    }
}
