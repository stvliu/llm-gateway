/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.gateway;

import com.codingas.gateway.domain.iam.entity.User;
import java.util.List;
import java.util.Optional;

/**
 * 用户网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface UserGateway {

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
     * 根据邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);

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
     * 删除用户
     *
     * @param user 用户实体
     */
    void delete(User user);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);
}
