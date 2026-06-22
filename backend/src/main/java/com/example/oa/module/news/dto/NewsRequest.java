package com.example.oa.module.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewsRequest {

    @NotBlank(message = "新闻标题不能为空")
    private String title;
    private String summary;

    @NotBlank(message = "新闻正文不能为空")
    private String content;

    private String category;
    private String coverImage;
    private String status;
    private Integer isTop;
    private Integer aiGenerated;
}
