package com.example.oa.module.dict.controller;

import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.dict.dto.DictDataQueryRequest;
import com.example.oa.module.dict.dto.DictDataRequest;
import com.example.oa.module.dict.entity.DictData;
import com.example.oa.module.dict.service.DictDataService;
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
@RequestMapping("/api/dict-data")
@RequiredArgsConstructor
public class DictDataController {

    private final DictDataService dictDataService;

    @GetMapping("/page")
    public Result<PageResult<DictData>> page(DictDataQueryRequest request) {
        return Result.success(dictDataService.pageData(request));
    }

    @GetMapping("/type/{typeCode}")
    public Result<List<DictData>> byType(@PathVariable String typeCode) {
        return Result.success(dictDataService.listByTypeCode(typeCode));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictData> create(@Valid @RequestBody DictDataRequest request) {
        return Result.success(dictDataService.createData(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictData> update(@PathVariable Long id, @Valid @RequestBody DictDataRequest request) {
        return Result.success(dictDataService.updateData(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        DictData data = dictDataService.getById(id);
        dictDataService.removeById(id);
        if (data != null) {
            dictDataService.clearTypeCache(data.getTypeCode());
        }
        return Result.success(null);
    }

    @PostMapping("/cache/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> refreshCache() {
        dictDataService.refreshCache();
        return Result.success(null);
    }
}
