package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.RouteGroupGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型路由服务
 *
 * <p>负责根据策略选择最优模型。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouterService {

    private static final String DEFAULT_MODEL_CODE = "openai/gpt-4o";

    private final ModelGateway modelGateway;
    private final RouteGroupGateway routeGroupGateway;

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

        Model model = modelGateway.findByModelCode(modelCode);
        if (model != null && model.getStatus() == Model.ModelStatus.ACTIVE) {
            return model;
        }

        log.warn("Model not found or inactive: modelCode={}, falling back to default", modelCode);
        return selectDefaultModel();
    }

    /**
     * 选择默认模型
     *
     * @return 默认模型
     */
    public Model selectDefaultModel() {
        Model defaultModel = modelGateway.findByModelCode(DEFAULT_MODEL_CODE);
        if (defaultModel != null && defaultModel.getStatus() == Model.ModelStatus.ACTIVE) {
            return defaultModel;
        }

        List<Model> activeModels = modelGateway.findAllActive();
        return activeModels.stream()
            .filter(m -> m.getStatus() == Model.ModelStatus.ACTIVE)
            .findFirst()
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