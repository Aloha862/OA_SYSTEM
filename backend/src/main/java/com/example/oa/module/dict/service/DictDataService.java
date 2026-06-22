package com.example.oa.module.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.dict.dto.DictDataQueryRequest;
import com.example.oa.module.dict.dto.DictDataRequest;
import com.example.oa.module.dict.entity.DictData;

import java.util.List;

public interface DictDataService extends IService<DictData> {

    PageResult<DictData> pageData(DictDataQueryRequest request);

    List<DictData> listByTypeCode(String typeCode);

    DictData createData(DictDataRequest request);

    DictData updateData(Long id, DictDataRequest request);

    void refreshCache();

    void clearTypeCache(String typeCode);
}
