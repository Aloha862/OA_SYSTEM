package com.example.oa.module.dict.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.cache.CacheSupport;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.dict.dto.DictDataQueryRequest;
import com.example.oa.module.dict.dto.DictDataRequest;
import com.example.oa.module.dict.entity.DictData;
import com.example.oa.module.dict.mapper.DictDataMapper;
import com.example.oa.module.dict.service.DictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictData> implements DictDataService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheSupport cacheSupport;

    @Override
    public PageResult<DictData> pageData(DictDataQueryRequest request) {
        String keyword = request.getKeyword();
        Page<DictData> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<DictData>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(DictData::getDictLabel, keyword)
                                .or()
                                .like(DictData::getDictValue, keyword)
                                .or()
                                .like(DictData::getTypeCode, keyword))
                        .eq(StringUtils.hasText(request.getTypeCode()), DictData::getTypeCode, request.getTypeCode())
                        .like(StringUtils.hasText(request.getDictLabel()), DictData::getDictLabel, request.getDictLabel())
                        .eq(StringUtils.hasText(request.getDictValue()), DictData::getDictValue, request.getDictValue())
                        .eq(request.getStatus() != null, DictData::getStatus, request.getStatus())
                        .orderByAsc(DictData::getSortOrder, DictData::getId));
        return PageResult.of(page);
    }

    @Override
    public List<DictData> listByTypeCode(String typeCode) {
        String key = CacheConstants.DICT_TYPE_PREFIX + typeCode;
        return cacheSupport.getOrLoad(key, CacheConstants.DICT_TTL, java.time.Duration.ofMinutes(2), () ->
                list(new LambdaQueryWrapper<DictData>()
                        .eq(DictData::getTypeCode, typeCode)
                        .eq(DictData::getStatus, 1)
                        .orderByAsc(DictData::getSortOrder, DictData::getId)).stream()
                        .collect(Collectors.toMap(
                                item -> item.getTypeCode() + ":" + item.getDictValue() + ":" + item.getDictLabel(),
                                Function.identity(), (left, right) -> left, java.util.LinkedHashMap::new))
                        .values().stream().toList(), List::isEmpty);
    }

    @Override
    public DictData createData(DictDataRequest request) {
        DictData dictData = BeanUtil.copyProperties(request, DictData.class);
        if (dictData.getStatus() == null) {
            dictData.setStatus(1);
        }
        if (dictData.getSortOrder() == null) {
            dictData.setSortOrder(0);
        }
        save(dictData);
        clearTypeCache(dictData.getTypeCode());
        return dictData;
    }

    @Override
    public DictData updateData(Long id, DictDataRequest request) {
        DictData old = getById(id);
        DictData dictData = BeanUtil.copyProperties(request, DictData.class);
        dictData.setId(id);
        updateById(dictData);
        clearTypeCache(dictData.getTypeCode());
        if (old != null) {
            clearTypeCache(old.getTypeCode());
        }
        return dictData;
    }

    @Override
    public void refreshCache() {
        list().stream().map(DictData::getTypeCode).distinct().forEach(this::clearTypeCache);
    }

    @Override
    public void clearTypeCache(String typeCode) {
        try {
            cacheSupport.deleteAfterCommit(CacheConstants.DICT_TYPE_PREFIX + typeCode);
        } catch (Exception e) {
            log.warn("清理字典缓存失败: typeCode={}", typeCode, e);
        }
    }
}
