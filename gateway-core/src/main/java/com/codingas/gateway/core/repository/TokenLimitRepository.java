package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.TokenLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TokenLimit 仓储接口
 */
@Repository
public interface TokenLimitRepository extends JpaRepository<TokenLimit, Long> {

    /**
     * 通过 limitCode 查找
     */
    Optional<TokenLimit> findByLimitCode(String limitCode);

    /**
     * 通过用户 ID 查找所有限额
     */
    List<TokenLimit> findByUserId(Long userId);

    /**
     * 通过用户 ID 和 Provider ID 查找
     */
    @Query("SELECT tl FROM TokenLimit tl WHERE tl.userId = :userId " +
           "AND (tl.providerId = :providerId OR tl.providerId IS NULL) " +
           "ORDER BY tl.providerId DESC NULLS LAST")
    List<TokenLimit> findByUserIdAndProviderId(@Param("userId") Long userId, @Param("providerId") Long providerId);

    /**
     * 检查限额编码是否存在
     */
    boolean existsByLimitCode(String limitCode);
}
