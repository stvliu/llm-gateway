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
package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.infrastructure.iam.gateway.encryption.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化阶段的用户创建工厂
 *
 * <p>封装 {@link User} 实体的构建与持久化逻辑，
 * 消除 {@link BuiltinUserLoader} 和 {@link SampleDataLoader} 中重复的用户创建代码。</p>
 */
@Component
public class UserCreator {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;

    UserCreator(UserGateway userGateway, PasswordEncoder passwordEncoder) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建并持久化用户
     *
     * @param username 用户名
     * @param email    邮箱
     * @param rawPassword 明文密码（将被加密存储）
     * @param role     角色
     * @param builtin  是否为内建用户
     * @return 持久化后的用户实体
     */
    User create(String username, String email, String rawPassword, String role, boolean builtin) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setState(UserState.ACTIVE);
        user.setBuiltin(builtin);
        return userGateway.save(user);
    }
}
