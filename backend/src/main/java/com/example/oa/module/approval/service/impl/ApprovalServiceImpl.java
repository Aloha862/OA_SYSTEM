package com.example.oa.module.approval.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.service.AiService;
import com.example.oa.module.approval.dto.ApprovalAuditRequest;
import com.example.oa.module.approval.dto.ApprovalQueryRequest;
import com.example.oa.module.approval.dto.ApprovalRequest;
import com.example.oa.module.approval.entity.Approval;
import com.example.oa.module.approval.entity.ApprovalRecord;
import com.example.oa.module.approval.enums.ApprovalStatusEnum;
import com.example.oa.module.approval.mapper.ApprovalMapper;
import com.example.oa.module.approval.mapper.ApprovalRecordMapper;
import com.example.oa.module.approval.service.ApprovalService;
import com.example.oa.module.department.service.DepartmentService;
import com.example.oa.module.notification.dto.NotificationMessage;
import com.example.oa.module.notification.mq.NotificationProducer;
import com.example.oa.security.service.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl extends ServiceImpl<ApprovalMapper, Approval> implements ApprovalService {

    private final ApprovalRecordMapper approvalRecordMapper;
    private final DepartmentService departmentService;
    private final NotificationProducer notificationProducer;
    private final AiService aiService;

    @Override
    public PageResult<Approval> pageApprovals(ApprovalQueryRequest request) {
        LambdaQueryWrapper<Approval> query = buildQuery(request);
        if (!SecurityUtils.isAdmin()) {
            Long userId = SecurityUtils.currentUserId();
            query.and(wrapper -> wrapper.eq(Approval::getApplicantId, userId).or().eq(Approval::getApproverId, userId));
        }
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), query));
    }

    @Override
    public PageResult<Approval> myApprovals(ApprovalQueryRequest request) {
        request.setApplicantId(SecurityUtils.currentUserId());
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), buildQuery(request)));
    }

    @Override
    public PageResult<Approval> todoApprovals(ApprovalQueryRequest request) {
        request.setApproverId(SecurityUtils.currentUserId());
        request.setStatus(ApprovalStatusEnum.PENDING.name());
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), buildQuery(request)));
    }

    @Override
    public PageResult<Approval> doneApprovals(ApprovalQueryRequest request) {
        Long userId = SecurityUtils.currentUserId();
        List<Long> ids = approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getOperatorId, userId)
                        .in(ApprovalRecord::getAction, List.of("APPROVE", "REJECT")))
                .stream().map(ApprovalRecord::getApprovalId).distinct().toList();
        if (ids.isEmpty()) {
            return new PageResult<>(List.of(), 0, request.getSize(), request.getCurrent(), 0);
        }
        LambdaQueryWrapper<Approval> query = buildQuery(request).in(Approval::getId, ids);
        return PageResult.of(page(new Page<>(request.getCurrent(), request.getSize()), query));
    }

    @Override
    public Approval detail(Long id) {
        Approval approval = getRequired(id);
        ensureVisible(approval);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Approval createApproval(ApprovalRequest request) {
        LoginUser loginUser = SecurityUtils.currentUser();
        Approval approval = BeanUtil.copyProperties(request, Approval.class);
        approval.setApprovalNo(generateApprovalNo());
        approval.setStatus(ApprovalStatusEnum.DRAFT.name());
        approval.setApplicantId(loginUser.getId());
        approval.setDepartmentId(loginUser.getDepartmentId());
        approval.setApproverId(departmentService.resolveApprover(loginUser.getDepartmentId()));
        save(approval);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Approval updateApproval(Long id, ApprovalRequest request) {
        Approval approval = getRequired(id);
        ensureApplicantOrAdmin(approval);
        if (!ApprovalStatusEnum.DRAFT.name().equals(approval.getStatus())) {
            throw new BusinessException("只有草稿审批可以修改");
        }
        BeanUtil.copyProperties(request, approval, "id", "approvalNo", "status", "applicantId", "departmentId", "approverId");
        updateById(approval);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApproval(Long id) {
        Approval approval = getRequired(id);
        ensureApplicantOrAdmin(approval);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApprovals(List<Long> ids) {
        ids.forEach(this::deleteApproval);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        Approval approval = getRequired(id);
        ensureApplicantOrAdmin(approval);
        if (!ApprovalStatusEnum.DRAFT.name().equals(approval.getStatus())
                && !ApprovalStatusEnum.WITHDRAWN.name().equals(approval.getStatus())) {
            throw new BusinessException("当前审批不能提交");
        }
        if (approval.getApproverId() == null) {
            approval.setApproverId(departmentService.resolveApprover(approval.getDepartmentId()));
        }
        if (approval.getApproverId() == null) {
            throw new BusinessException("未配置部门审批人或负责人");
        }
        approval.setStatus(ApprovalStatusEnum.PENDING.name());
        approval.setSubmittedAt(LocalDateTime.now());
        updateById(approval);
        addRecord(id, "SUBMIT", "提交审批");
        notificationProducer.send("oa.notification.approval",
                new NotificationMessage(approval.getApproverId(), approval.getApplicantId(), "新的审批待处理",
                        approval.getTitle(), "approval.submit", "APPROVAL", approval.getId(), LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        Approval approval = getRequired(id);
        ensureApplicantOrAdmin(approval);
        if (!ApprovalStatusEnum.PENDING.name().equals(approval.getStatus())) {
            throw new BusinessException("只有待审批状态可以撤回");
        }
        approval.setStatus(ApprovalStatusEnum.WITHDRAWN.name());
        updateById(approval);
        addRecord(id, "WITHDRAW", "撤回审批");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, ApprovalAuditRequest request) {
        audit(id, request, ApprovalStatusEnum.APPROVED.name(), "APPROVE", "审批通过", "approval.approved");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, ApprovalAuditRequest request) {
        audit(id, request, ApprovalStatusEnum.REJECTED.name(), "REJECT", "审批驳回", "approval.rejected");
    }

    @Override
    public List<ApprovalRecord> records(Long id) {
        detail(id);
        return approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getApprovalId, id)
                .orderByAsc(ApprovalRecord::getId));
    }

    @Override
    public AiResponse aiSummary(Long id) {
        Approval approval = getRequired(id);
        ensureCanApprove(approval);
        AiResponse response = aiService.approvalSummary(approval);
        approval.setAiSummary(response.getContent());
        updateById(approval);
        return response;
    }

    @Override
    public AiResponse aiRisk(Long id) {
        Approval approval = getRequired(id);
        ensureCanApprove(approval);
        AiResponse response = aiService.approvalRiskAnalyze(approval);
        Object data = response.getData();
        if (data instanceof java.util.Map<?, ?> map) {
            Object level = map.get("riskLevel");
            Object suggestion = map.get("suggestion");
            approval.setAiRiskLevel(level == null ? null : String.valueOf(level));
            approval.setAiRiskSuggestion(suggestion == null ? response.getContent() : String.valueOf(suggestion));
        } else {
            approval.setAiRiskSuggestion(response.getContent());
        }
        updateById(approval);
        return response;
    }

    private void audit(Long id, ApprovalAuditRequest request, String status, String action, String title, String type) {
        Approval approval = getRequired(id);
        ensureCanApprove(approval);
        if (!ApprovalStatusEnum.PENDING.name().equals(approval.getStatus())) {
            throw new BusinessException("只有待审批状态可以处理");
        }
        approval.setStatus(status);
        approval.setApprovedAt(LocalDateTime.now());
        updateById(approval);
        addRecord(id, action, request == null ? null : request.getComment());
        notificationProducer.send("oa.notification.approval",
                new NotificationMessage(approval.getApplicantId(), SecurityUtils.currentUserId(), title,
                        approval.getTitle(), type, "APPROVAL", approval.getId(), LocalDateTime.now()));
    }

    private void ensureApplicantOrAdmin(Approval approval) {
        if (!SecurityUtils.isAdmin() && !Objects.equals(approval.getApplicantId(), SecurityUtils.currentUserId())) {
            throw new BusinessException(403, "只能操作自己的审批");
        }
    }

    private void ensureVisible(Approval approval) {
        Long userId = SecurityUtils.currentUserId();
        boolean visible = SecurityUtils.isAdmin()
                || Objects.equals(approval.getApplicantId(), userId)
                || Objects.equals(approval.getApproverId(), userId);
        if (!visible) {
            throw new BusinessException(403, "Only related users can view this approval");
        }
    }

    private void ensureCanApprove(Approval approval) {
        LoginUser user = SecurityUtils.currentUser();
        boolean admin = SecurityUtils.isAdmin();
        boolean configuredApprover = approval.getApproverId() != null && approval.getApproverId().equals(user.getId());
        boolean approverIdentity = user.getIsApprover() != null && user.getIsApprover() == 1;
        if (!admin && !(configuredApprover && approverIdentity)) {
            throw new BusinessException(403, "当前用户无审批权限");
        }
    }

    private void addRecord(Long approvalId, String action, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setApprovalId(approvalId);
        record.setOperatorId(SecurityUtils.currentUserId());
        record.setAction(action);
        record.setComment(comment);
        approvalRecordMapper.insert(record);
    }

    private Approval getRequired(Long id) {
        Approval approval = getById(id);
        if (approval == null) {
            throw new BusinessException("审批不存在");
        }
        return approval;
    }

    private LambdaQueryWrapper<Approval> buildQuery(ApprovalQueryRequest request) {
        String keyword = request.getKeyword();
        return new LambdaQueryWrapper<Approval>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Approval::getApprovalNo, keyword)
                        .or()
                        .like(Approval::getTitle, keyword)
                        .or()
                        .like(Approval::getReason, keyword))
                .like(StringUtils.hasText(request.getApprovalNo()), Approval::getApprovalNo, request.getApprovalNo())
                .like(StringUtils.hasText(request.getTitle()), Approval::getTitle, request.getTitle())
                .eq(StringUtils.hasText(request.getType()), Approval::getType, request.getType())
                .eq(StringUtils.hasText(request.getStatus()), Approval::getStatus, request.getStatus())
                .eq(request.getApplicantId() != null, Approval::getApplicantId, request.getApplicantId())
                .eq(request.getApproverId() != null, Approval::getApproverId, request.getApproverId())
                .eq(request.getDepartmentId() != null, Approval::getDepartmentId, request.getDepartmentId())
                .orderByDesc(Approval::getId);
    }

    private String generateApprovalNo() {
        return "APP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(4);
    }
}
