package com.example.oa.module.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class OaFile extends BaseEntity {

    private String originalName;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String extension;
    private String businessType;
    private Long businessId;
    private Long uploaderId;
    private Integer downloadCount;
}
