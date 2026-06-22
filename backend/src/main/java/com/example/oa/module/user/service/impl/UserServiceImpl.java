package com.example.oa.module.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oa.common.constant.CacheConstants;
import com.example.oa.common.constant.SecurityConstants;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.exception.UnauthorizedException;
import com.example.oa.common.result.PageResult;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.department.entity.Department;
import com.example.oa.module.department.mapper.DepartmentMapper;
import com.example.oa.module.user.dto.LoginRequest;
import com.example.oa.module.user.dto.PasswordResetRequest;
import com.example.oa.module.user.dto.RegisterRequest;
import com.example.oa.module.user.dto.UserCreateRequest;
import com.example.oa.module.user.dto.UserProfileRequest;
import com.example.oa.module.user.dto.UserQueryRequest;
import com.example.oa.module.user.dto.UserUpdateRequest;
import com.example.oa.module.user.entity.User;
import com.example.oa.module.user.mapper.UserMapper;
import com.example.oa.module.user.service.UserService;
import com.example.oa.module.user.vo.AuthResponse;
import com.example.oa.module.user.vo.UserVO;
import com.example.oa.security.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse login(LoginRequest request) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("账号已被禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, toVO(user));
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(CacheConstants.JWT_BLACKLIST_PREFIX + token, 1,
                    jwtTokenUtil.expirationMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    @Override
    public AuthResponse refresh(String token) {
        if (!StringUtils.hasText(token) || !jwtTokenUtil.validateToken(token)) {
            throw new UnauthorizedException("Token无效");
        }
        User user = getById(jwtTokenUtil.userId(token));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("用户不存在或已禁用");
        }
        return new AuthResponse(jwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getRole()), toVO(user));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (count(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole("EMPLOYEE");
        user.setStatus(0);
        user.setGender(0);
        user.setIsApprover(0);
        save(user);
        return toVO(user);
    }

    @Override
    public UserVO me() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("未登录");
        }
        return detail(userId);
    }

    @Override
    public PageResult<UserVO> pageUsers(UserQueryRequest request) {
        Page<User> page = page(new Page<>(request.getCurrent(), request.getSize()), buildQuery(request));
        IPage<UserVO> voPage = page.convert(this::toVO);
        return PageResult.of(voPage);
    }

    @Override
    public UserVO detail(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserCreateRequest request) {
        if (count(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = BeanUtil.copyProperties(request, User.class);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(request.getPassword()) ? request.getPassword() : "123456"));
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("EMPLOYEE");
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getGender() == null) {
            user.setGender(0);
        }
        if (user.getIsApprover() == null) {
            user.setIsApprover(0);
        }
        save(user);
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        User user = getRequired(id);
        BeanUtil.copyProperties(request, user, "id", "username", "password");
        updateById(user);
        return toVO(user);
    }

    @Override
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = getRequired(id);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        updateById(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = getRequired(id);
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public void updateApprover(Long id, boolean approver) {
        User user = getRequired(id);
        user.setIsApprover(approver ? 1 : 0);
        updateById(user);
    }

    @Override
    public List<UserVO> listApprovers() {
        return list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .eq(User::getIsApprover, 1)
                .orderByAsc(User::getRealName))
                .stream().map(this::toVO).toList();
    }

    @Override
    public List<UserVO> listActiveOptions() {
        return list(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getRealName)
                .orderByAsc(User::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public UserVO updateProfile(UserProfileRequest request) {
        User user = getRequired(SecurityUtils.currentUserId());
        user.setRealName(request.getRealName());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setAvatar(request.getAvatar());
        user.setPosition(request.getPosition());
        updateById(user);
        return toVO(user);
    }

    private LambdaQueryWrapper<User> buildQuery(UserQueryRequest request) {
        String keyword = request.getKeyword();
        return new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getRealName, keyword)
                        .or()
                        .like(User::getPhone, keyword))
                .like(StringUtils.hasText(request.getUsername()), User::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getRealName()), User::getRealName, request.getRealName())
                .eq(StringUtils.hasText(request.getRole()), User::getRole, request.getRole())
                .eq(request.getStatus() != null, User::getStatus, request.getStatus())
                .eq(request.getDepartmentId() != null, User::getDepartmentId, request.getDepartmentId())
                .eq(request.getIsApprover() != null, User::getIsApprover, request.getIsApprover())
                .orderByDesc(User::getId);
    }

    private User getRequired(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private UserVO toVO(User user) {
        UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
        if (user.getDepartmentId() != null) {
            Department department = departmentMapper.selectById(user.getDepartmentId());
            if (department != null) {
                vo.setDepartmentName(department.getName());
            }
        }
        return vo;
    }

    public static String stripBearer(String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return authorization.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return authorization;
    }
}
