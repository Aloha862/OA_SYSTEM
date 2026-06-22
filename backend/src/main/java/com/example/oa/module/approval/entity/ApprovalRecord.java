package com.example.oa.module.approval.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval_record")
public class ApprovalRecord extends BaseEntity {

    private Long approvalId;
    private Long operatorId;
    private String action;
    private String comment;
}
