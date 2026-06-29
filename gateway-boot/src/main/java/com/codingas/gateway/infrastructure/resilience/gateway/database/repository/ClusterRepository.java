package com.codingas.gateway.infrastructure.resilience.gateway.database.repository;

import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ClusterDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Cluster 故障域 JPA Repository
 */
public interface ClusterRepository extends JpaRepository<ClusterDo, Long> {

    /**
     * 按故障域编码查找
     *
     * @param code 故障域编码
     * @return 命中的 DO；不存在时返回空
     */
    Optional<ClusterDo> findByCode(String code);
}
