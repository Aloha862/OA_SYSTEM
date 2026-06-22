package com.example.oa.module.dict.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeQueryRequest extends PageQuery {

    private String keyword;
    private String typeCode;
    private String typeName;
    private Integer status;
}
