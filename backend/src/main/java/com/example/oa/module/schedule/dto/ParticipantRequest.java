package com.example.oa.module.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ParticipantRequest {

    @NotEmpty(message = "参与人不能为空")
    private List<Long> userIds;
}
