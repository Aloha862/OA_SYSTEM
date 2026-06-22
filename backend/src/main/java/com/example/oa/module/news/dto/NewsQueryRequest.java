package com.example.oa.module.news.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NewsQueryRequest extends PageQuery {

    private String keyword;
    private String title;
    private String category;
    private String status;
}
