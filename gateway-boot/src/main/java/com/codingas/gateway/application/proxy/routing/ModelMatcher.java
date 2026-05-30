package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型匹配器 — 根据 modelName 查找 Model
 */
@Component
@RequiredArgsConstructor
public class ModelMatcher {

    private final ModelGateway modelGateway;

    /**
     * 根据 modelName 查找匹配的活跃模型
     *
     * @param modelName 模型名称（对应 Model.modelName）
     * @return 匹配的 Model
     * @throws ResourceNotFoundException 未找到匹配模型
     */
    public Model match(String modelName) {
        return modelGateway.findByModelName(modelName)
                .filter(m -> m.getState() == ModelState.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Model", modelName));
    }
}
