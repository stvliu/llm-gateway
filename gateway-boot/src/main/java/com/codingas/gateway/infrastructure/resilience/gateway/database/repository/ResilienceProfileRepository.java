package com.codingas.gateway.infrastructure.resilience.gateway.database.repository;

import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ResilienceProfileDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 容灾画像 JPA Repository
 */
public interface ResilienceProfileRepository extends JpaRepository<ResilienceProfileDo, Long> {

    /**
     * 按画像编码查找
     *
     * @param code 画像编码
     * @return 命中的 DO；不存在时返回空
     */
    Optional<ResilienceProfileDo> findByCode(String code);
}
