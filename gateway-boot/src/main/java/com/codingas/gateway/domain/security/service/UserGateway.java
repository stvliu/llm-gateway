package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.User;

import java.util.Optional;

/**
 * 用户网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface UserGateway {

    /**
     * 根据 ID 查找用户
     *
     * @param id 用户 ID
     * @return 用户信息，不存在返回 null
     */
    Optional<User> findById(Long id);

    /**
     * 保存用户
     *
     * @param user 用户实体
     * @return 保存后的实体
     */
    User save(User user);
}
