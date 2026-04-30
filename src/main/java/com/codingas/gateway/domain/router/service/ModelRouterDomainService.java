package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.RouteGroupGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * 模型路由服务
 *
 * <p>负责根据策略选择最优模型。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouterDomainService {

    private final ModelGateway modelGateway;
    private final RouteGroupGateway routeGroupGateway;
    private final GatewayProperties properties;

    /**
     * 根据模型代码选择模型
     *
     * @param modelCode 模型代码
     * @return 模型信息
     */
    @Cacheable(value = "model", key = "#modelCode ?: 'default'")
    public Model selectModel(String modelCode) {
        if (modelCode == null || modelCode.isBlank()) {
            return selectDefaultModel();
        }

        return modelGateway.findByModelCode(modelCode)
            .filter(m -> m.getStatus() == Model.ModelStatus.ACTIVE)
            .orElseGet(() -> {
                log.warn("Model not found or inactive: modelCode={}, falling back to default", modelCode);
                return selectDefaultModel();
            });
    }

    /**
     * 选择默认模型
     *
     * @return 默认模型
     */
    public Model selectDefaultModel() {
        String defaultModelCode = properties.getRouter().getDefaultModelCode();
        return ofNullable(modelGateway.findByModelCode(defaultModelCode)
            .filter(m -> m.getStatus() == Model.ModelStatus.ACTIVE)
            .orElseGet(() -> {
                List<Model> activeModels = modelGateway.findAllActive();
                return activeModels.stream()
                    .filter(m -> m.getStatus() == Model.ModelStatus.ACTIVE)
                    .findFirst()
                    .orElse(null);
            }))
            .orElseThrow(() -> new IllegalStateException("No active model available"));
    }

    /**
     * 根据路由分组选择模型
     *
     * @param groupCode 分组代码
     * @param strategy 路由策略
     * @return 模型信息
     */
    public Model selectModelByRouteGroup(String groupCode, RouteGroup.RoutingStrategy strategy) {
        RouteGroup group = routeGroupGateway.findByGroupCode(groupCode);
        if (group == null || !Boolean.TRUE.equals(group.getEnabled())) {
            log.debug("Route group not found or disabled: groupCode={}, using default", groupCode);
            return selectModel(null);
        }

        // TODO: 根据策略选择模型
        log.debug("Route group selected: groupCode={}, strategy={}", groupCode, strategy);
        return selectModel(null);
    }

    /**
     * 获取所有活跃模型
     *
     * @return 活跃模型列表
     */
    public List<Model> getAllActiveModels() {
        return modelGateway.findAllActive();
    }
}
