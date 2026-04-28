package com.codingas.gateway.application.user;

import com.codingas.gateway.adapter.admin.dto.user.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.entity.UserRole;
import com.codingas.gateway.domain.security.entity.Role;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.gateway.RoleGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户应用服务
 *
 * <p>处理用户管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplication {

    private final UserGateway userGateway;
    private final RoleGateway roleGateway;

    /**
     * 创建用户
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        // 检查邮箱唯一性
        if (userGateway.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email");
        }

        // 生成用户编码
        String userCode = generateUserCode();

        // 创建用户
        User user = new User();
        user.setUserCode(userCode);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(hashPassword(request.getPassword()));
        user.setPhone(request.getPhone());

        User savedUser = userGateway.save(user);

        // 分配角色
        assignRoles(savedUser, request.getRoleCodes());

        return toResponse(savedUser);
    }

    /**
     * 根据 ID 获取用户
     */
    public UserResponse getById(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    /**
     * 查询用户列表
     */
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

        if (request.getStatus() != null) {
            users = users.stream()
                .filter(u -> u.getStatus() == request.getStatus())
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
     */
    @Transactional
    public void delete(Long id) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setDeletedAt(Instant.now());
        userGateway.save(user);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setStatus(request.getStatus());
        return toResponse(userGateway.save(user));
    }

    /**
     * 分配用户角色
     */
    @Transactional
    public UserResponse assignRoles(Long id, UserRoleAssignRequest request) {
        User user = userGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        assignRoles(user, request.getRoleCodes());

        return toResponse(user);
    }

    /**
     * 分配角色辅助方法
     */
    private void assignRoles(User user, List<String> roleCodes) {
        // 清除现有角色
        user.getUserRoles().clear();

        // 查找角色
        List<Role> roles = roleGateway.findByRoleCodes(roleCodes);
        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            user.getUserRoles().add(userRole);
        }

        userGateway.save(user);
    }

/**
     * 生成用户编码
     */
    private String generateUserCode() {
        return "USR" + System.currentTimeMillis();
    }

    /**
     * 密码哈希 (使用 SHA-256)
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 转换为响应 DTO
     */
    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUserCode(user.getUserCode());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus());
        response.setEmailVerified(user.getEmailVerified());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        // 转换角色
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            List<UserResponse.RoleInfo> roleInfos = user.getUserRoles().stream()
                .map(ur -> {
                    UserResponse.RoleInfo info = new UserResponse.RoleInfo();
                    info.setRoleCode(ur.getRole().getRoleCode());
                    info.setName(ur.getRole().getName());
                    return info;
                })
                .collect(Collectors.toList());
            response.setRoles(roleInfos);
        }

        return response;
    }
}
