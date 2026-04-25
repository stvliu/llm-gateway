package com.codingas.gateway.security.authorization;

import com.codingas.gateway.core.domain.entity.User;
import com.codingas.gateway.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * RBAC 权限服务
 *
 * <p>提供基于角色的访问控制，支持管理员/普通用户/只读用户三种角色。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private final UserRepository userRepository;

    /**
     * 检查用户是否具有指定权限
     */
    public boolean hasPermission(Long userId, String permission) {
        if (userId == null || permission == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() == null) {
            return false;
        }

        return switch (user.getRole()) {
            case ADMIN -> true; // 管理员拥有所有权限
            case USER -> permission.startsWith("api:"); // 普通用户只能访问 API
            case READONLY -> permission.equals("api:read"); // 只读用户只能读取
        };
    }

    /**
     * 检查用户是否具有指定角色
     */
    public boolean hasRole(Long userId, User.UserRole role) {
        if (userId == null || role == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == role;
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        return hasRole(userId, User.UserRole.ADMIN);
    }

    /**
     * 获取用户的所有权限
     */
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Set.of();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() == null) {
            return Set.of();
        }

        return switch (user.getRole()) {
            case ADMIN -> Set.of("*");
            case USER -> Set.of("api:call", "api:read");
            case READONLY -> Set.of("api:read");
        };
    }

    /**
     * 检查用户是否可以访问指定模型
     *
     * @param userId 用户ID
     * @param model 模型名称（如 "gpt-4", "claude-3-opus"）
     * @param whitelist 模型白名单（JSON数组，null表示允许全部）
     */
    public boolean canAccessModel(Long userId, String model, String whitelist) {
        if (isAdmin(userId)) {
            return true; // 管理员可以访问所有模型
        }

        if (whitelist == null || whitelist.isBlank()) {
            return true; // 没有白名单，允许全部
        }

        // 解析白名单并检查
        // 简单实现，实际应该用 JSON 解析库
        return whitelist.contains("\"" + model + "\"") || whitelist.contains(model);
    }

    /**
     * 检查用户是否在 IP 白名单内
     *
     * @param userId 用户ID
     * @param ipAddress 客户端 IP
     * @param whitelist IP 白名单（JSON数组，null表示允许全部）
     */
    public boolean isIpAllowed(Long userId, String ipAddress, String whitelist) {
        if (whitelist == null || whitelist.isBlank()) {
            return true; // 没有白名单，允许全部
        }

        // 解析白名单并检查
        // 简化实现，实际应该支持 CIDR 格式
        return whitelist.contains("\"" + ipAddress + "\"") || whitelist.contains(ipAddress);
    }
}
