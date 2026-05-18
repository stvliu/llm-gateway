package com.codingas.gateway.domain.team.gateway;

import com.codingas.gateway.domain.team.entity.UserApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key Gateway 接口
 */
public interface UserApiKeyGateway {

    UserApiKey save(UserApiKey apiKey);

    Optional<UserApiKey> findById(Long id);

    Optional<UserApiKey> findByKeyHash(String keyHash);

    List<UserApiKey> findByTeamId(Long teamId);

    List<UserApiKey> findByProductId(Long productId);

    void deleteById(Long id);

    long countByTeamId(Long teamId);
}
