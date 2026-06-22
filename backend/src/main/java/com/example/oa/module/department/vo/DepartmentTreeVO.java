package com.example.oa.module.department.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentTreeVO {

    private Long id;
    private String name;
    private Long parentId;
    private Long leaderId;
    private Long approverId;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DepartmentTreeVO> children = new ArrayList<>();
}
