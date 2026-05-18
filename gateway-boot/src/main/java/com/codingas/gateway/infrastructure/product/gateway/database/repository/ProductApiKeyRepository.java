package com.codingas.gateway.infrastructure.product.gateway.database.repository;

import com.codingas.gateway.infrastructure.product.gateway.database.dataobject.ProductApiKeyDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 产品 API Key Repository
 */
@Repository
public interface ProductApiKeyRepository extends JpaRepository<ProductApiKeyDo, Long> {

    @Query("SELECT k FROM ProductApiKeyDo k WHERE k.productId = :productId AND k.state = 'active' ORDER BY k.priority ASC")
    List<ProductApiKeyDo> findActiveByProductId(@Param("productId") Long productId);

    @Query("SELECT k FROM ProductApiKeyDo k WHERE k.productId = :productId AND k.state = 'active' ORDER BY k.priority ASC LIMIT 1")
    Optional<ProductApiKeyDo> findDefaultByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("UPDATE ProductApiKeyDo k SET k.lastUsedAt = :lastUsedAt WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    long countByProductIdAndState(Long productId, String state);
}