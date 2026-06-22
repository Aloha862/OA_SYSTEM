package com.example.oa.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oa.common.result.PageResult;
import com.example.oa.module.user.dto.LoginRequest;
import com.example.oa.module.user.dto.PasswordResetRequest;
import com.example.oa.module.user.dto.RegisterRequest;
import com.example.oa.module.user.dto.UserCreateRequest;
import com.example.oa.module.user.dto.UserProfileRequest;
import com.example.oa.module.user.dto.UserQueryRequest;
import com.example.oa.module.user.dto.UserUpdateRequest;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.vo.AuthResponse;
import com.example.oa.module.user.vo.UserVO;

import java.util.List;

public interface UserService extends IService<User> {

    AuthResponse login(LoginRequest request);

    void logout(String token);

    AuthResponse refresh(String token);

    UserVO register(RegisterRequest request);

    UserVO me();

    PageResult<UserVO> pageUsers(UserQueryRequest request);

    UserVO detail(Long id);

    UserVO createUser(UserCreateRequest request);

    UserVO updateUser(Long id, UserUpdateRequest request);

    void resetPassword(Long id, PasswordResetRequest request);

    void updateStatus(Long id, Integer status);

    void updateApprover(Long id, boolean approver);

    List<UserVO> listApprovers();

    List<UserVO> listActiveOptions();

    UserVO updateProfile(UserProfileRequest request);
}
