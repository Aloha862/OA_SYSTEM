package com.example.oa.module.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.dict.dto.DictTypeQueryRequest;
import com.example.oa.module.dict.dto.DictTypeRequest;
import com.example.oa.module.dict.entity.DictType;

public interface DictTypeService extends IService<DictType> {

    PageResult<DictType> pageTypes(DictTypeQueryRequest request);

    DictType createType(DictTypeRequest request);

    DictType updateType(Long id, DictTypeRequest request);
}
