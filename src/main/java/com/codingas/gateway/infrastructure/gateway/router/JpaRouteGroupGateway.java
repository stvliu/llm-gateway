package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.RouteGroupGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 路由分组网关 JPA 实现
 *
 * <p>实现 RouteGroupGateway 接口，使用 JPA 进行持久化。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaRouteGroupGateway implements RouteGroupGateway {

    private final RouteGroupRepository repository;

    @Override
    public RouteGroup findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public RouteGroup findByGroupCode(String groupCode) {
        return repository.findByGroupCode(groupCode).orElse(null);
    }

    @Override
    public List<RouteGroup> findAllActive() {
        return repository.findAllActive();
    }

    @Override
    public RouteGroup save(RouteGroup routeGroup) {
        return repository.save(routeGroup);
    }
}

/**
 * 路由分组仓储接口
 */
interface RouteGroupRepository {
    java.util.Optional<RouteGroup> findById(Long id);
    java.util.Optional<RouteGroup> findByGroupCode(String groupCode);
    List<RouteGroup> findAllActive();
    RouteGroup save(RouteGroup routeGroup);
}