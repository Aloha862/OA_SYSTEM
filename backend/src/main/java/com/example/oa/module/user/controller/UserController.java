package com.example.oa.module.user.controller;

import com.example.oa.common.dto.IdListRequest;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.result.Result;
import com.example.oa.module.user.dto.ApproverUpdateRequest;
import com.example.oa.module.user.dto.PasswordResetRequest;
import com.example.oa.module.user.dto.StatusUpdateRequest;
import com.example.oa.module.user.dto.UserCreateRequest;
import com.example.oa.module.user.dto.UserProfileRequest;
import com.example.oa.module.user.dto.UserQueryRequest;
import com.example.oa.module.user.dto.UserUpdateRequest;
import com.example.oa.module.user.service.UserService;
import com.example.oa.module.user.vo.UserVO;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<UserVO>> page(UserQueryRequest request) {
        return Result.success(userService.pageUsers(request));
    }

    @GetMapping("/options")
    public Result<List<UserVO>> options() {
        return Result.success(userService.listActiveOptions());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success(null);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@Valid @RequestBody IdListRequest request) {
        userService.removeByIds(request.getIds());
        return Result.success(null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success(null);
    }

    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return Result.success(null);
    }

    @PutMapping("/{id}/approver")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateApprover(@PathVariable Long id, @Valid @RequestBody ApproverUpdateRequest request) {
        userService.updateApprover(id, request.getApprover());
        return Result.success(null);
    }

    @GetMapping("/approvers")
    public Result<List<UserVO>> approvers() {
        return Result.success(userService.listApprovers());
    }

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.me());
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UserProfileRequest request) {
        return Result.success(userService.updateProfile(request));
    }
}
