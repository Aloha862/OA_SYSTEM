package com.example.oa.module.ai.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiLogQueryRequest extends PageQuery {

    private Long userId;
    private String functionType;
    private String provider;
}
