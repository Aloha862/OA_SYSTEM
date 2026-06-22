package com.example.oa.module.news.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.oa.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_news_favorite")
public class NewsFavorite extends BaseEntity {

    private Long newsId;
    private Long userId;
}
