package com.example.oa.module.news.controller;

import com.example.oa.common.dto.IdListRequest;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.news.dto.NewsCommentRequest;
import com.example.oa.module.news.dto.NewsQueryRequest;
import com.example.oa.module.news.dto.NewsRequest;
import com.example.oa.module.news.entity.News;
import com.example.oa.module.news.entity.NewsComment;
import com.example.oa.module.news.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/page")
    public Result<PageResult<News>> page(NewsQueryRequest request) {
        return Result.success(newsService.pageNews(request));
    }

    @GetMapping("/favorites/my")
    public Result<List<News>> favorites() {
        return Result.success(newsService.myFavorites());
    }

    @GetMapping("/{id}")
    public Result<News> detail(@PathVariable Long id) {
        return Result.success(newsService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<News> create(@Valid @RequestBody NewsRequest request) {
        return Result.success(newsService.createNews(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<News> update(@PathVariable Long id, @Valid @RequestBody NewsRequest request) {
        return Result.success(newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.removeById(id);
        newsService.clearNewsCache(id);
        return Result.success(null);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@Valid @RequestBody IdListRequest request) {
        newsService.removeByIds(request.getIds());
        request.getIds().forEach(newsService::clearNewsCache);
        return Result.success(null);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publish(@PathVariable Long id) {
        newsService.publish(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> offline(@PathVariable Long id) {
        newsService.offline(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> top(@PathVariable Long id) {
        newsService.top(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/view")
    public Result<Void> view(@PathVariable Long id) {
        newsService.view(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/comments")
    public Result<NewsComment> comment(@PathVariable Long id, @Valid @RequestBody NewsCommentRequest request) {
        return Result.success(newsService.comment(id, request));
    }

    @GetMapping("/{id}/comments")
    public Result<List<NewsComment>> comments(@PathVariable Long id) {
        return Result.success(newsService.comments(id));
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        newsService.deleteComment(commentId);
        return Result.success(null);
    }

    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        newsService.like(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        newsService.unlike(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        newsService.favorite(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        newsService.unfavorite(id);
        return Result.success(null);
    }

    @PostMapping("/ai-generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AiResponse> aiGenerate(@Valid @RequestBody NewsGenerateRequest request) {
        return Result.success(newsService.generate(request));
    }

    @PostMapping("/ai-polish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AiResponse> aiPolish(@Valid @RequestBody NewsPolishRequest request) {
        return Result.success(newsService.polish(request));
    }
}
