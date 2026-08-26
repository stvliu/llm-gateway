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

import com.codingas.gateway.iam.user.User;
import java.util.List;
import java.util.Optional;

/**
 * 用户网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface UserRepository {

    /**
     * 保存用户
     *
     * @param user 用户实体
     * @return 保存后的实体
     */
    User save(User user);

    /**
     * 根据 ID 查找用户
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    Optional<User> findById(Long id);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 统计用户总数
     *
     * @return 用户数量
     */
    long count();

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);
}
