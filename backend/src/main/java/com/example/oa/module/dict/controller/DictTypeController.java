package com.example.oa.module.dict.controller;

import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.dict.dto.DictTypeQueryRequest;
import com.example.oa.module.dict.dto.DictTypeRequest;
import com.example.oa.module.dict.entity.DictType;
import com.example.oa.module.dict.service.DictTypeService;
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

@RestController
@RequestMapping("/api/dict-types")
@RequiredArgsConstructor
public class DictTypeController {

    private final DictTypeService dictTypeService;

    @GetMapping("/page")
    public Result<PageResult<DictType>> page(DictTypeQueryRequest request) {
        return Result.success(dictTypeService.pageTypes(request));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictType> create(@Valid @RequestBody DictTypeRequest request) {
        return Result.success(dictTypeService.createType(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DictType> update(@PathVariable Long id, @Valid @RequestBody DictTypeRequest request) {
        return Result.success(dictTypeService.updateType(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        dictTypeService.removeById(id);
        return Result.success(null);
    }
}
