package com.example.oa.module.department.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_department")
public class Department extends BaseEntity {

    private String name;
    private Long parentId;
    private Long leaderId;
    private Long approverId;
    private Integer sortOrder;
    private Integer status;
}
