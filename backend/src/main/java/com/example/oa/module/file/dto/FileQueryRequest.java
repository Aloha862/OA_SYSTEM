package com.example.oa.module.file.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileQueryRequest extends PageQuery {

    private String businessType;
    private Long businessId;
    private Long uploaderId;
}
