package com.codingas.gateway.infrastructure.iam.gateway.database.repository;

import com.codingas.gateway.infrastructure.iam.gateway.database.dataobject.UserApiKeyChannelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * UserApiKey-Channel 关联 JPA Repository
 */
public interface UserApiKeyChannelRepository extends JpaRepository<UserApiKeyChannelDo, Long> {

    List<UserApiKeyChannelDo> findByUserApiKeyId(Long userApiKeyId);

    @Query("SELECT DISTINCT c.channelId FROM UserApiKeyChannelDo c WHERE c.userApiKeyId = :userApiKeyId")
    List<Long> findChannelIdByUserApiKeyId(@Param("userApiKeyId") Long userApiKeyId);

    @Query("SELECT DISTINCT c.userApiKeyId FROM UserApiKeyChannelDo c WHERE c.channelId = :channelId")
    List<Long> findUserApiKeyIdByChannelId(@Param("channelId") Long channelId);

    @Modifying
    @Query("DELETE FROM UserApiKeyChannelDo c WHERE c.userApiKeyId = :userApiKeyId")
    void deleteByUserApiKeyId(@Param("userApiKeyId") Long userApiKeyId);
}
