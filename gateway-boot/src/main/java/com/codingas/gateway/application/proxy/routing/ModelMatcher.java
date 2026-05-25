package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型匹配器 — 根据 modelName 查找 ModelSpec
 */
@Component
@RequiredArgsConstructor
public class ModelMatcher {

    private final ModelSpecGateway modelSpecGateway;

    /**
     * 根据 modelName 查找匹配的活跃模型规格
     *
     * @param modelName 模型名称（对应 ModelSpec.providerModelId）
     * @return 匹配的 ModelSpec
     * @throws ResourceNotFoundException 未找到匹配模型
     */
    public ModelSpec match(String modelName) {
        return modelSpecGateway.findByProviderModelId(modelName)
                .filter(m -> m.getState() == ModelSpecState.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("ModelSpec", modelName));
    }
}
