package com.codingas.gateway.config.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.codingas.gateway.core.domain.entity.User;
import com.codingas.gateway.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限接口实现
 *
 * <p>提供用户权限和角色数据给 Sa-Token 框架。</p>
 */
@Component
@RequiredArgsConstructor
public class ApiKeyStpInterface implements StpInterface {

    private final UserRepository userRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 获取用户权限列表
        if (loginId == null) {
            return new ArrayList<>();
        }

        User user = userRepository.findById((Long) loginId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        List<String> permissions = new ArrayList<>();

        // 根据角色添加权限
        if (user.getRole() != null) {
            permissions.add("role:" + user.getRole().name().toLowerCase());

            switch (user.getRole()) {
                case ADMIN -> {
                    permissions.add("*"); // 管理员拥有所有权限
                }
                case USER -> {
                    permissions.add("api:call");
                    permissions.add("api:read");
                }
                case READONLY -> {
                    permissions.add("api:read");
                }
            }
        }

        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 获取用户角色列表
        if (loginId == null) {
            return new ArrayList<>();
        }

        User user = userRepository.findById((Long) loginId).orElse(null);
        if (user == null || user.getRole() == null) {
            return new ArrayList<>();
        }

        return List.of(user.getRole().name());
    }
}
