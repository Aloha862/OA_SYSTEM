package com.example.oa.module.department.controller;

import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.department.dto.DepartmentLeaderRequest;
import com.example.oa.module.department.dto.DepartmentQueryRequest;
import com.example.oa.module.department.dto.DepartmentRequest;
import com.example.oa.module.department.entity.Department;
import com.example.oa.module.department.service.DepartmentService;
import com.example.oa.module.department.vo.DepartmentTreeVO;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping("/tree")
    public Result<List<DepartmentTreeVO>> tree() {
        return Result.success(departmentService.tree());
    }

    @GetMapping("/page")
    public Result<PageResult<Department>> page(DepartmentQueryRequest request) {
        return Result.success(departmentService.pageDepartments(request));
    }

    @GetMapping("/{id}")
    public Result<Department> detail(@PathVariable Long id) {
        return Result.success(departmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Department> create(@Valid @RequestBody DepartmentRequest request) {
        return Result.success(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Department> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return Result.success(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.removeById(id);
        departmentService.clearTreeCache();
        return Result.success(null);
    }

    @PutMapping("/{id}/leader")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateLeader(@PathVariable Long id, @Valid @RequestBody DepartmentLeaderRequest request) {
        departmentService.updateLeader(id, request.getUserId());
        return Result.success(null);
    }

    @PutMapping("/{id}/approver")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateApprover(@PathVariable Long id, @Valid @RequestBody DepartmentLeaderRequest request) {
        departmentService.updateApprover(id, request.getUserId());
        return Result.success(null);
    }
}
