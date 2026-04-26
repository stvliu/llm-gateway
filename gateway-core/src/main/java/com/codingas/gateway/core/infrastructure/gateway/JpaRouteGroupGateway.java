package com.codingas.gateway.core.infrastructure.gateway;

import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.core.domain.gateway.RouteGroupGateway;
import com.codingas.gateway.core.repository.RouteGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 路由分组网关实现
 *
 * <p>实现 RouteGroupGateway 接口，使用 JPA 进行持久化。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaRouteGroupGateway implements RouteGroupGateway {

    private final RouteGroupRepository repository;

    @Override
    public Optional<RouteGroup> findByGroupCode(String groupCode) {
        return repository.findByGroupCode(groupCode);
    }

    @Override
    public Optional<RouteGroup> findById(Long groupId) {
        return repository.findById(groupId);
    }

    @Override
    public List<RouteGroup> findAllActive() {
        return repository.findAll();
    }

    @Override
    public RouteGroup save(RouteGroup routeGroup) {
        return repository.save(routeGroup);
    }
}
