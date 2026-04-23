package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.RouteGroupProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RouteGroupProvider 仓储接口
 */
@Repository
public interface RouteGroupProviderRepository extends JpaRepository<RouteGroupProvider, Long> {

    /**
     * 通过 RouteGroupId 查找所有 Provider 关联
     */
    List<RouteGroupProvider> findByRouteGroupId(Long routeGroupId);

    /**
     * 通过 RouteGroupId 查找可用的 Provider 关联
     */
    @Query("SELECT rgp FROM RouteGroupProvider rgp WHERE rgp.routeGroupId = :routeGroupId " +
           "AND rgp.status = 'ENABLED' ORDER BY rgp.priority DESC")
    List<RouteGroupProvider> findEnabledByRouteGroupIdOrderByPriorityDesc(@Param("routeGroupId") Long routeGroupId);

    /**
     * 通过 ProviderId 查找所有 RouteGroup 关联
     */
    List<RouteGroupProvider> findByProviderId(Long providerId);
}
