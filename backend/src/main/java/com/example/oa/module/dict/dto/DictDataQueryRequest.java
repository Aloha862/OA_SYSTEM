package com.example.oa.module.dict.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictDataQueryRequest extends PageQuery {

    private String keyword;
    private String typeCode;
    private String dictLabel;
    private String dictValue;
    private Integer status;
}
