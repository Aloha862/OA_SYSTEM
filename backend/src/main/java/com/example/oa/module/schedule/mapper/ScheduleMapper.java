package com.example.oa.module.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oa.module.schedule.entity.Schedule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface ScheduleMapper extends BaseMapper<Schedule> {

    @Update("""
            UPDATE oa_schedule
            SET status = 'FINISHED', updated_at = #{now}
            WHERE deleted = 0
              AND status = 'NORMAL'
              AND end_time IS NOT NULL
              AND end_time <= #{now}
            """)
    int finishExpired(@Param("now") LocalDateTime now);
}
