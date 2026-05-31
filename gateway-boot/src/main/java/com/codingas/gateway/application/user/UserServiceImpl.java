package com.codingas.gateway.application.user;

import com.codingas.gateway.application.auth.dto.ChangePasswordRequest;
import com.codingas.gateway.application.auth.dto.LoginRequest;
import com.codingas.gateway.application.auth.dto.LoginResponse;
import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户应用服务实现
 *
 * <p>处理用户管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户
     */
    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        // 检查邮箱唯一性
        if (userGateway.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());

        // 设置角色（默认为 USER）
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole());
        }

        User savedUser = userGateway.save(user);

        return toResponse(savedUser);
    }

    /**
     * 根据 ID 获取用户
     */
    @Override
    public UserResponse getById(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    /**
     * 查询用户列表
     */
    @Override
    public PageResponse<UserResponse> query(UserQueryRequest request) {
        List<User> users = userGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            users = users.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(keyword)
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());
        }

        if (request.getState() != null) {
            users = users.stream()
                .filter(u -> u.getState() == request.getState())
                .collect(Collectors.toList());
        }

        // 统计
        long total = users.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<User> pagedUsers = users.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<UserResponse> responses = pagedUsers.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新用户
     */
    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toResponse(userGateway.save(user));
    }

    /**
     * 删除用户（软删除）
     *
     * <p>禁止删除内建用户，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isBuiltin()) {
            throw new ForbiddenException("不允许删除系统内建用户");
        }

        user.setDeletedAt(Instant.now());
        userGateway.save(user);
    }

    /**
     * 更新用户状态
     *
     * <p>禁止禁用内建用户，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public UserResponse updateState(Long id, UserStateUpdateRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isBuiltin() && request.getState() != UserState.ACTIVE) {
            throw new ForbiddenException("不允许禁用系统内建用户");
        }

        user.setState(request.getState());
        return toResponse(userGateway.save(user));
    }

    /**
     * 分配用户角色
     *
     * <p>简化角色模型：直接设置 User.role 字段。</p>
     * <p>禁止变更内建用户的角色，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public UserResponse assignRoles(Long id, UserRoleAssignRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            String roleCode = request.getRoleCodes().get(0);

            // 禁止变更内建用户的角色
            if (user.isBuiltin()) {
                throw new ForbiddenException("不允许变更系统内建用户的角色");
            }

            user.setRole(roleCode);
        }

        return toResponse(userGateway.save(user));
    }

    /**
     * 用户登录
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userGateway.findByUsername(request.username())
            .orElseThrow(() -> new AuthenticationFailedException("用户名或密码错误"));

        // 检查用户状态
        if (!user.isActive()) {
            throw new AuthenticationFailedException("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(Instant.now());
        userGateway.save(user);

        // 使用 SaToken 登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 构建响应
        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole()
        );

        return new LoginResponse(userResponse, token);
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userGateway.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 验证当前密码
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userGateway.save(user);
    }

    /**
     * 用户登出
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 转换为响应 DTO
     */
    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setState(user.getState());
        response.setEmailVerified(user.getEmailVerified());
        response.setRole(user.getRole());
        response.setBuiltin(user.getBuiltin());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }
}