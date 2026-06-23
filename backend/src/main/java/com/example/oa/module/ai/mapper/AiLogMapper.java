package com.example.oa.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.oa.module.ai.entity.AiLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface AiLogMapper extends BaseMapper<AiLog> {
    @Delete("DELETE FROM sys_ai_log WHERE created_at < #{before}")
    int purgeBefore(@Param("before") LocalDateTime before);
}
