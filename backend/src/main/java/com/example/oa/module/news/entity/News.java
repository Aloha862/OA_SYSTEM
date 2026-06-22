package com.example.oa.module.news.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_news")
public class News extends BaseEntity {

    private String title;
    private String summary;
    private String content;
    private String category;
    private String coverImage;
    private String status;
    @TableField("is_top")
    private Integer isTop;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private Long publisherId;
    private LocalDateTime publishedAt;
    private Integer aiGenerated;
}
