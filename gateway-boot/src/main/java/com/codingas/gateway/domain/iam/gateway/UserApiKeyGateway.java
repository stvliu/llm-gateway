package com.codingas.gateway.domain.iam.gateway;

import com.codingas.gateway.domain.iam.entity.UserApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key 领域网关接口
 */
public interface UserApiKeyGateway {

    /** 按 ID 查找 */
    Optional<UserApiKey> findById(Long id);

    /** 按用户 ID 查找 */
    List<UserApiKey> findByUserId(Long userId);

    /** 查询所有非删除状态的 Key（管理员用） */
    List<UserApiKey> findAllNonDeleted();

    /** 按 Key 前缀查找（认证用） */
    Optional<UserApiKey> findByKeyPrefix(String keyPrefix);

    /** 保存（含渠道关联） */
    UserApiKey save(UserApiKey userApiKey);

    /** 删除 */
    void delete(UserApiKey userApiKey);
}