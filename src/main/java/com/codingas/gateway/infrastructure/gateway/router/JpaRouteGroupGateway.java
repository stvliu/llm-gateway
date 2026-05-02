package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.RouteGroupGateway;
import com.codingas.gateway.infrastructure.router.RouteGroupDo;
import com.codingas.gateway.infrastructure.router.RouteGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 路由分组网关 JPA 实现
 *
 * <p>实现 RouteGroupGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaRouteGroupGateway implements RouteGroupGateway {

    private final RouteGroupRepository repository;

    @Override
    public RouteGroup findById(Long id) {
        return repository.findById(id).map(this::toEntity).orElse(null);
    }

    @Override
    public RouteGroup findByGroupCode(String groupCode) {
        return repository.findByGroupCode(groupCode).map(this::toEntity).orElse(null);
    }

    @Override
    public List<RouteGroup> findAllActive() {
        return repository.findAllActive().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public RouteGroup save(RouteGroup routeGroup) {
        RouteGroupDo doEntity = toDo(routeGroup);
        RouteGroupDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    /**
     * DO 转 Entity
     */
    private RouteGroup toEntity(RouteGroupDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        RouteGroup entity = new RouteGroup();
        entity.setId(doEntity.getId());
        entity.setGroupCode(doEntity.getGroupCode());
        entity.setGroupName(doEntity.getGroupName());
        entity.setEnabled(doEntity.getEnabled());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getStrategy() != null) {
            entity.setStrategy(RouteGroup.RoutingStrategy.valueOf(doEntity.getStrategy().name()));
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private RouteGroupDo toDo(RouteGroup entity) {
        if (entity == null) {
            return null;
        }
        RouteGroupDo doEntity = new RouteGroupDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setGroupCode(entity.getGroupCode());
        doEntity.setGroupName(entity.getGroupName());
        doEntity.setEnabled(entity.getEnabled());
        // 枚举转换
        if (entity.getStrategy() != null) {
            doEntity.setStrategy(RouteGroupDo.RoutingStrategy.valueOf(entity.getStrategy().name()));
        }
        return doEntity;
    }
}