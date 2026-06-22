package com.example.oa.module.department.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.department.dto.DepartmentQueryRequest;
import com.example.oa.module.department.dto.DepartmentRequest;
import com.example.oa.module.department.entity.Department;
import com.example.oa.module.department.mapper.DepartmentMapper;
import com.example.oa.module.department.service.DepartmentService;
import com.example.oa.module.department.vo.DepartmentTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @SuppressWarnings("unchecked")
    public List<DepartmentTreeVO> tree() {
        try {
            Object cached = redisTemplate.opsForValue().get(CacheConstants.DEPARTMENT_TREE);
            if (cached instanceof List<?>) {
                return (List<DepartmentTreeVO>) cached;
            }
        } catch (Exception ignored) {
        }
        List<DepartmentTreeVO> tree = buildTree(list(new LambdaQueryWrapper<Department>()
                .eq(Department::getStatus, 1)
                .orderByAsc(Department::getSortOrder, Department::getId)));
        try {
            redisTemplate.opsForValue().set(CacheConstants.DEPARTMENT_TREE, tree);
        } catch (Exception ignored) {
        }
        return tree;
    }

    @Override
    public PageResult<Department> pageDepartments(DepartmentQueryRequest request) {
        String keyword = request.getKeyword();
        Page<Department> page = page(new Page<>(request.getCurrent(), request.getSize()),
                new LambdaQueryWrapper<Department>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(Department::getName, keyword))
                        .like(StringUtils.hasText(request.getName()), Department::getName, request.getName())
                        .eq(request.getParentId() != null, Department::getParentId, request.getParentId())
                        .eq(request.getStatus() != null, Department::getStatus, request.getStatus())
                        .orderByAsc(Department::getSortOrder, Department::getId));
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department createDepartment(DepartmentRequest request) {
        Department department = BeanUtil.copyProperties(request, Department.class);
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        if (department.getSortOrder() == null) {
            department.setSortOrder(0);
        }
        save(department);
        clearTreeCache();
        return department;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Department updateDepartment(Long id, DepartmentRequest request) {
        Department department = getRequired(id);
        BeanUtil.copyProperties(request, department, "id");
        updateById(department);
        clearTreeCache();
        return department;
    }

    @Override
    public void updateLeader(Long id, Long leaderId) {
        Department department = getRequired(id);
        department.setLeaderId(leaderId);
        updateById(department);
        clearTreeCache();
    }

    @Override
    public void updateApprover(Long id, Long approverId) {
        Department department = getRequired(id);
        department.setApproverId(approverId);
        updateById(department);
        clearTreeCache();
    }

    @Override
    public Long resolveApprover(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        Department department = getById(departmentId);
        if (department == null) {
            return null;
        }
        return department.getApproverId() != null ? department.getApproverId() : department.getLeaderId();
    }

    @Override
    public void clearTreeCache() {
        try {
            redisTemplate.delete(CacheConstants.DEPARTMENT_TREE);
        } catch (Exception ignored) {
        }
    }

    private Department getRequired(Long id) {
        Department department = getById(id);
        if (department == null) {
            throw new BusinessException("部门不存在");
        }
        return department;
    }

    private List<DepartmentTreeVO> buildTree(List<Department> departments) {
        List<DepartmentTreeVO> nodes = departments.stream()
                .map(department -> BeanUtil.copyProperties(department, DepartmentTreeVO.class))
                .sorted(Comparator.comparing(DepartmentTreeVO::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<Long, DepartmentTreeVO> nodeMap = nodes.stream().collect(Collectors.toMap(DepartmentTreeVO::getId, node -> node));
        List<DepartmentTreeVO> roots = new ArrayList<>();
        for (DepartmentTreeVO node : nodes) {
            Long parentId = node.getParentId() == null ? 0L : node.getParentId();
            if (parentId == 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        return roots;
    }
}
