package com.example.oa.module.user.controller;

import com.example.oa.common.result.Result;
import com.example.oa.module.user.dto.LoginRequest;
import com.example.oa.module.user.dto.RegisterRequest;
import com.example.oa.module.user.service.UserService;
import com.example.oa.module.user.service.impl.UserServiceImpl;
import com.example.oa.module.user.vo.AuthResponse;
import com.example.oa.module.user.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        userService.logout(UserServiceImpl.stripBearer(authorization));
        return Result.success(null);
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.me());
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.success(userService.refresh(UserServiceImpl.stripBearer(authorization)));
    }
}
