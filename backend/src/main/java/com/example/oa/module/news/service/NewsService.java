package com.example.oa.module.news.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.news.dto.NewsCommentRequest;
import com.example.oa.module.news.dto.NewsQueryRequest;
import com.example.oa.module.news.dto.NewsRequest;
import com.example.oa.module.news.entity.News;
import com.example.oa.module.news.entity.NewsComment;

import java.util.List;

public interface NewsService extends IService<News> {

    PageResult<News> pageNews(NewsQueryRequest request);

    News detail(Long id);

    News createNews(NewsRequest request);

    News updateNews(Long id, NewsRequest request);

    void publish(Long id);

    void offline(Long id);

    void top(Long id);

    void view(Long id);

    NewsComment comment(Long id, NewsCommentRequest request);

    List<NewsComment> comments(Long id);

    void deleteComment(Long commentId);

    void like(Long id);

    void unlike(Long id);

    void favorite(Long id);

    void unfavorite(Long id);

    List<News> myFavorites();

    AiResponse generate(NewsGenerateRequest request);

    AiResponse polish(NewsPolishRequest request);

    void clearNewsCache(Long id);
}
