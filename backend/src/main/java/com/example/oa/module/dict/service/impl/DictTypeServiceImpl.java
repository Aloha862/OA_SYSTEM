package com.example.oa.module.dict.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.dict.dto.DictTypeQueryRequest;
import com.example.oa.module.dict.dto.DictTypeRequest;
import com.example.oa.module.dict.entity.DictType;
import com.example.oa.module.dict.mapper.DictTypeMapper;
import com.example.oa.module.dict.service.DictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, DictType> implements DictTypeService {

    @Override
    public PageResult<DictType> pageTypes(DictTypeQueryRequest request) {
        String keyword = request.getKeyword();
        Page<DictType> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<DictType>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(DictType::getTypeCode, keyword)
                                .or()
                                .like(DictType::getTypeName, keyword))
                        .like(StringUtils.hasText(request.getTypeCode()), DictType::getTypeCode, request.getTypeCode())
                        .like(StringUtils.hasText(request.getTypeName()), DictType::getTypeName, request.getTypeName())
                        .eq(request.getStatus() != null, DictType::getStatus, request.getStatus())
                        .orderByDesc(DictType::getId));
        return PageResult.of(page);
    }

    @Override
    public DictType createType(DictTypeRequest request) {
        DictType dictType = BeanUtil.copyProperties(request, DictType.class);
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        save(dictType);
        return dictType;
    }

    @Override
    public DictType updateType(Long id, DictTypeRequest request) {
        DictType dictType = BeanUtil.copyProperties(request, DictType.class);
        dictType.setId(id);
        updateById(dictType);
        return dictType;
    }
}
