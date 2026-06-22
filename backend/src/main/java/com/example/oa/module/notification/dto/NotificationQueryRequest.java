package com.example.oa.module.notification.dto;

import com.example.oa.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQueryRequest extends PageQuery {

    private String keyword;
    private Integer readStatus;
    private String type;
}
