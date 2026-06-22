package com.example.oa.module.department.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.department.dto.DepartmentQueryRequest;
import com.example.oa.module.department.dto.DepartmentRequest;
import com.example.oa.module.department.entity.Department;
import com.example.oa.module.department.vo.DepartmentTreeVO;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    List<DepartmentTreeVO> tree();

    PageResult<Department> pageDepartments(DepartmentQueryRequest request);

    Department createDepartment(DepartmentRequest request);

    Department updateDepartment(Long id, DepartmentRequest request);

    void updateLeader(Long id, Long leaderId);

    void updateApprover(Long id, Long approverId);

    Long resolveApprover(Long departmentId);

    void clearTreeCache();
}
