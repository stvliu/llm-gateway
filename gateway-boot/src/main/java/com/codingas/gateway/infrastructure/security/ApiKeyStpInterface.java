package com.codingas.gateway.infrastructure.security;

import cn.dev33.satoken.stp.StpInterface;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限接口实现
 *
 * <p>提供用户权限和角色数据给 Sa-Token 框架。</p>
 */
/**
 * @deprecated 旧架构权限接口，新架构由 TeamRole 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
@Component
@RequiredArgsConstructor
public class ApiKeyStpInterface implements StpInterface {

    private final UserGateway userGateway;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 获取用户权限列表
        if (loginId == null) {
            return new ArrayList<>();
        }

        User user = userGateway.findById((Long) loginId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        List<String> permissions = new ArrayList<>();
        // TODO: 从 UserRole 关联中获取权限
        // 目前暂时返回基础权限
        permissions.add("api:call");
        permissions.add("api:read");

        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 获取用户角色列表
        if (loginId == null) {
            return new ArrayList<>();
        }

        User user = userGateway.findById((Long) loginId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        // TODO: 从 UserRole 关联中获取角色
        // 目前暂时返回空列表
        return new ArrayList<>();
    }
}