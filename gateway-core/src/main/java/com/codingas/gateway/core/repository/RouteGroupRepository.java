package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.RouteGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RouteGroup 仓储接口
 */
@Repository
public interface RouteGroupRepository extends JpaRepository<RouteGroup, Long> {

    /**
     * 通过 groupCode 查找
     */
    Optional<RouteGroup> findByGroupCode(String groupCode);
}
