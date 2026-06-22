package com.example.oa.module.department.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DepartmentQueryRequest extends PageQuery {

    private String keyword;
    private String name;
    private Long parentId;
    private Integer status;
}
