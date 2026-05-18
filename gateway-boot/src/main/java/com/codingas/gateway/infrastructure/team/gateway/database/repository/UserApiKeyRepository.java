package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key Repository
 */
@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKeyDo, Long> {

    Optional<UserApiKeyDo> findByKeyHash(String keyHash);

    List<UserApiKeyDo> findByTeamId(Long teamId);

    List<UserApiKeyDo> findByProductId(Long productId);

    long countByTeamId(Long teamId);
}
