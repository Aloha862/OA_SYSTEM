package com.example.oa.module.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewsCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;
    private Long parentId;
}
