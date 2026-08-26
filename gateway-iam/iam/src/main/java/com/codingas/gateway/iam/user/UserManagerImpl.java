/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.user;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.auth.RolePermissions;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.iam.encryption.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 *
 * <p>处理用户管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagerImpl implements UserManager {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 重置密码字符集：排除易混字符 O/0/I/1/l */
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int RESET_PASSWORD_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 创建用户
     */
    @Override
    @Transactional
    public User create(User user, String plainPassword) {
        // 检查邮箱唯一性
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email");
        }

        // 明文密码编码为哈希后存储
        user.setPasswordHash(passwordEncoder.encode(plainPassword));

        // 设置角色（默认为 USER）
        if (user.getRole() != null && !user.getRole().isBlank()) {
            user.setRole(user.getRole());
        }

        return userRepository.save(user);
    }

    /**
     * 根据 ID 获取用户
     */
    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * 查询用户列表
     */
    @Override
    public PageResponse<User> query(UserQuery query) {
        List<User> users = userRepository.findAll();

        // 过滤
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String keyword = query.getKeyword().toLowerCase();
            users = users.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(keyword)
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());
        }

        if (query.getState() != null) {
            users = users.stream()
                .filter(u -> u.getState() == query.getState())
                .collect(Collectors.toList());
        }

        // 统计
        long total = users.size();

        // 分页
        int offset = query.getOffset();
        int limit = query.getLimit();
        List<User> pagedUsers = users.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        return PageResponse.of(pagedUsers, query.getPage(), limit, total);
    }

    /**
     * 更新用户
     */
    @Override
    @Transactional
    public User update(Long id, User user) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // 实体 null 字段表示不更新
        if (user.getUsername() != null) {
            existing.setUsername(user.getUsername());
        }
        if (user.getEmail() != null) {
            existing.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existing.setPhone(user.getPhone());
        }
        if (user.getAvatarUrl() != null) {
            existing.setAvatarUrl(user.getAvatarUrl());
        }

        return userRepository.save(existing);
    }

    /**
     * 删除用户（软删除）
     *
     * <p>禁止删除内建用户，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isBuiltin()) {
            throw new ForbiddenException("不允许删除系统内建用户");
        }

        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * 更新用户状态
     *
     * <p>禁止禁用内建用户，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public User updateState(Long id, UserState state) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (user.isBuiltin() && state != UserState.ACTIVE) {
            throw new ForbiddenException("不允许禁用系统内建用户");
        }

        user.setState(state);
        return userRepository.save(user);
    }

    /**
     * 分配用户角色
     *
     * <p>简化角色模型：直接设置 User.role 字段。</p>
     * <p>禁止变更内建用户的角色，防止系统失去管理入口。</p>
     */
    @Override
    @Transactional
    public User assignRoles(Long id, List<String> roleCodes) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (roleCodes != null && !roleCodes.isEmpty()) {
            String roleCode = roleCodes.get(0);

            // 禁止变更内建用户的角色
            if (user.isBuiltin()) {
                throw new ForbiddenException("不允许变更系统内建用户的角色");
            }

            user.setRole(roleCode);
        }

        return userRepository.save(user);
    }

    /**
     * 用户登录
     */
    @Override
    @Transactional
    public LoginResult login(String username, String password, boolean rememberMe) {
        // 查找用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationFailedException("用户名或密码错误"));

        // 检查用户状态
        if (!user.isActive()) {
            throw new AuthenticationFailedException("用户已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationFailedException("用户名或密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // 使用 SaToken 登录（授权基于 USER/ADMIN 角色，由 PermissionInterceptor 校验）
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 构建结果（携带角色推导的权限码，前端 UI 直接消费、不自行维护映射）
        return new LoginResult(user, token, RolePermissions.of(user.getRole()));
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 验证当前密码
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new GatewayRequestException("INVALID_PASSWORD", "当前密码错误");
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 重置密码（管理员触发）
     */
    @Override
    @Transactional
    public ResetPasswordResult resetPassword(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isBuiltin()) {
            throw new ForbiddenException("不允许重置系统内建用户的密码");
        }

        // 生成 16 位随机密码（排除易混字符）
        StringBuilder plain = new StringBuilder(RESET_PASSWORD_LENGTH);
        for (int i = 0; i < RESET_PASSWORD_LENGTH; i++) {
            plain.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        String plainPassword = plain.toString();

        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        userRepository.save(user);
        log.info("Reset password for user: id={}", userId);

        return new ResetPasswordResult(plainPassword);
    }

    /**
     * 用户登出
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }
}
