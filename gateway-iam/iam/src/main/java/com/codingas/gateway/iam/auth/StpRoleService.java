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
package com.codingas.gateway.iam.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sa-Token 角色/权限数据源
 *
 * <p>实现 {@link StpInterface}，{@code StpUtil.hasRole(...)} / {@code hasPermission(...)}
 * 会通过本 Bean 动态读取 users.role —— <b>角色级授权的必要基础设施</b>：
 * 若无此实现，Sa-Token 无法解析角色，{@code hasRole} 运行时恒为 false（授权全部锁死）。</p>
 *
 * <p>注册为 Spring Bean 后由 sa-token-spring-boot-starter 自动接管。</p>
 */
@Service
public class StpRoleService implements StpInterface {

    private final UserRepository userRepository;

    public StpRoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 按登录用户 ID 返回角色（ADMIN / USER，users.role 为唯一事实源）
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型（Sa-Token 预留）
     * @return 该用户的角色；用户不存在返回空集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return userRepository.findById(userId)
            .map(User::getRole)
            .map(List::of)
            .orElseGet(List::of);
    }

    /**
     * 按登录用户 ID 返回权限码（与登录响应推导一致）
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型（Sa-Token 预留）
     * @return 该用户角色对应的权限码；用户不存在返回空集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return userRepository.findById(userId)
            .map(User::getRole)
            .map(RolePermissions::of)
            .orElseGet(List::of);
    }
}
