package com.example.oa.module.news.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.cache.CacheSupport;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.dto.NewsGenerateRequest;
import com.example.oa.module.ai.dto.NewsPolishRequest;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.news.dto.NewsCommentRequest;
import com.example.oa.module.news.dto.NewsQueryRequest;
import com.example.oa.module.news.dto.NewsRequest;
import com.example.oa.module.news.entity.News;
import com.example.oa.module.news.entity.NewsComment;
import com.example.oa.module.news.entity.NewsFavorite;
import com.example.oa.module.news.entity.NewsLike;
import com.example.oa.module.news.enums.NewsStatusEnum;
import com.example.oa.module.news.mapper.NewsCommentMapper;
import com.example.oa.module.news.mapper.NewsFavoriteMapper;
import com.example.oa.module.news.mapper.NewsLikeMapper;
import com.example.oa.module.news.mapper.NewsMapper;
import com.example.oa.module.news.service.NewsService;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheSupport cacheSupport;
    private final NewsCommentMapper newsCommentMapper;
    private final NewsLikeMapper newsLikeMapper;
    private final NewsFavoriteMapper newsFavoriteMapper;
    private final NotificationProducer notificationProducer;
    private final UserMapper userMapper;
    private final AiService aiService;

    @Override
    public PageResult<News> pageNews(NewsQueryRequest request) {
        String keyword = request.getKeyword();
        LambdaQueryWrapper<News> query = new LambdaQueryWrapper<News>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(News::getTitle, keyword)
                        .or()
                        .like(News::getSummary, keyword)
                        .or()
                        .like(News::getContent, keyword))
                .like(StringUtils.hasText(request.getTitle()), News::getTitle, request.getTitle())
                .eq(StringUtils.hasText(request.getCategory()), News::getCategory, request.getCategory())
                .eq(StringUtils.hasText(request.getStatus()), News::getStatus, request.getStatus())
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishedAt)
                .orderByDesc(News::getId);
        if (!SecurityUtils.isAdmin()) {
            query.eq(News::getStatus, NewsStatusEnum.PUBLISHED.name());
        }
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), query));
    }

    @Override
    public News detail(Long id) {
        String key = CacheConstants.NEWS_DETAIL_PREFIX + id;
        News news = cacheSupport.getOrLoad(key, CacheConstants.NEWS_DETAIL_TTL, java.time.Duration.ofMinutes(1),
                () -> getRequired(id), value -> false);
        if (!SecurityUtils.isAdmin() && !NewsStatusEnum.PUBLISHED.name().equals(news.getStatus())) {
            throw new BusinessException(403, "新闻未发布");
        }
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News createNews(NewsRequest request) {
        News news = BeanUtil.copyProperties(request, News.class);
        news.setPublisherId(SecurityUtils.currentUserId());
        fillDefaults(news);
        save(news);
        clearNewsCache(news.getId());
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News updateNews(Long id, NewsRequest request) {
        News news = getRequired(id);
        BeanUtil.copyProperties(request, news, "id", "publisherId", "viewCount", "likeCount", "favoriteCount", "commentCount");
        fillDefaults(news);
        updateById(news);
        clearNewsCache(id);
        return news;
    }

    @Override
    public void publish(Long id) {
        News news = getRequired(id);
        news.setStatus(NewsStatusEnum.PUBLISHED.name());
        news.setPublishedAt(LocalDateTime.now());
        updateById(news);
        clearNewsCache(id);
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, 1))
                .forEach(user -> notificationProducer.send("oa.notification.news",
                        new NotificationMessage(user.getId(), SecurityUtils.currentUserId(), "新闻发布",
                                news.getTitle(), "news.publish", "NEWS", news.getId(), LocalDateTime.now())));
    }

    @Override
    public void offline(Long id) {
        News news = getRequired(id);
        news.setStatus(NewsStatusEnum.OFFLINE.name());
        updateById(news);
        clearNewsCache(id);
    }

    @Override
    public void top(Long id) {
        News news = getRequired(id);
        news.setIsTop(news.getIsTop() != null && news.getIsTop() == 1 ? 0 : 1);
        updateById(news);
        clearNewsCache(id);
    }

    @Override
    public void view(Long id) {
        News news = getRequired(id);
        news.setViewCount((news.getViewCount() == null ? 0 : news.getViewCount()) + 1);
        updateById(news);
        clearNewsCache(id);
    }

    @Override
    public NewsComment comment(Long id, NewsCommentRequest request) {
        getRequired(id);
        NewsComment comment = new NewsComment();
        comment.setNewsId(id);
        comment.setUserId(SecurityUtils.currentUserId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setStatus("PUBLISHED");
        newsCommentMapper.insert(comment);
        changeCounter(id, "commentCount", 1);
        return comment;
    }

    @Override
    public List<NewsComment> comments(Long id) {
        return newsCommentMapper.selectList(new LambdaQueryWrapper<NewsComment>()
                .eq(NewsComment::getNewsId, id)
                .eq(NewsComment::getStatus, "PUBLISHED")
                .orderByAsc(NewsComment::getId));
    }

    @Override
    public void deleteComment(Long commentId) {
        NewsComment comment = newsCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!SecurityUtils.isAdmin() && !comment.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        newsCommentMapper.deleteById(commentId);
        changeCounter(comment.getNewsId(), "commentCount", -1);
    }

    @Override
    public void like(Long id) {
        getRequired(id);
        if (newsLikeMapper.selectCount(new LambdaQueryWrapper<NewsLike>()
                .eq(NewsLike::getNewsId, id)
                .eq(NewsLike::getUserId, SecurityUtils.currentUserId())) == 0) {
            NewsLike like = new NewsLike();
            like.setNewsId(id);
            like.setUserId(SecurityUtils.currentUserId());
            newsLikeMapper.insert(like);
            changeCounter(id, "likeCount", 1);
        }
    }

    @Override
    public void unlike(Long id) {
        NewsLike like = newsLikeMapper.selectOne(new LambdaQueryWrapper<NewsLike>()
                .eq(NewsLike::getNewsId, id)
                .eq(NewsLike::getUserId, SecurityUtils.currentUserId()));
        if (like != null) {
            newsLikeMapper.deleteById(like.getId());
            changeCounter(id, "likeCount", -1);
        }
    }

    @Override
    public void favorite(Long id) {
        getRequired(id);
        if (newsFavoriteMapper.selectCount(new LambdaQueryWrapper<NewsFavorite>()
                .eq(NewsFavorite::getNewsId, id)
                .eq(NewsFavorite::getUserId, SecurityUtils.currentUserId())) == 0) {
            NewsFavorite favorite = new NewsFavorite();
            favorite.setNewsId(id);
            favorite.setUserId(SecurityUtils.currentUserId());
            newsFavoriteMapper.insert(favorite);
            changeCounter(id, "favoriteCount", 1);
        }
    }

    @Override
    public void unfavorite(Long id) {
        NewsFavorite favorite = newsFavoriteMapper.selectOne(new LambdaQueryWrapper<NewsFavorite>()
                .eq(NewsFavorite::getNewsId, id)
                .eq(NewsFavorite::getUserId, SecurityUtils.currentUserId()));
        if (favorite != null) {
            newsFavoriteMapper.deleteById(favorite.getId());
            changeCounter(id, "favoriteCount", -1);
        }
    }

    @Override
    public List<News> myFavorites() {
        List<Long> ids = newsFavoriteMapper.selectList(new LambdaQueryWrapper<NewsFavorite>()
                        .eq(NewsFavorite::getUserId, SecurityUtils.currentUserId()))
                .stream().map(NewsFavorite::getNewsId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return listByIds(ids);
    }

    @Override
    public AiResponse generate(NewsGenerateRequest request) {
        return aiService.generateNews(request);
    }

    @Override
    public AiResponse polish(NewsPolishRequest request) {
        return aiService.polishNews(request);
    }

    @Override
    public void clearNewsCache(Long id) {
        try {
            cacheSupport.deleteAfterCommit(CacheConstants.NEWS_DETAIL_PREFIX + id, CacheConstants.NEWS_PUBLISHED_LIST);
        } catch (Exception e) {
            log.warn("清理新闻缓存失败: newsId={}", id, e);
        }
    }

    private void fillDefaults(News news) {
        if (!StringUtils.hasText(news.getStatus())) {
            news.setStatus(NewsStatusEnum.DRAFT.name());
        }
        if (news.getIsTop() == null) {
            news.setIsTop(0);
        }
        if (news.getViewCount() == null) {
            news.setViewCount(0);
        }
        if (news.getLikeCount() == null) {
            news.setLikeCount(0);
        }
        if (news.getFavoriteCount() == null) {
            news.setFavoriteCount(0);
        }
        if (news.getCommentCount() == null) {
            news.setCommentCount(0);
        }
        if (news.getAiGenerated() == null) {
            news.setAiGenerated(0);
        }
    }

    private void changeCounter(Long id, String field, int delta) {
        News news = getRequired(id);
        if ("commentCount".equals(field)) {
            news.setCommentCount(Math.max(0, (news.getCommentCount() == null ? 0 : news.getCommentCount()) + delta));
        } else if ("likeCount".equals(field)) {
            news.setLikeCount(Math.max(0, (news.getLikeCount() == null ? 0 : news.getLikeCount()) + delta));
        } else if ("favoriteCount".equals(field)) {
            news.setFavoriteCount(Math.max(0, (news.getFavoriteCount() == null ? 0 : news.getFavoriteCount()) + delta));
        }
        updateById(news);
        clearNewsCache(id);
    }

    private News getRequired(Long id) {
        News news = getById(id);
        if (news == null) {
            throw new BusinessException("新闻不存在");
        }
        return news;
    }
}
