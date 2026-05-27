package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格领域服务
 *
 * <p>封装模型规格相关的核心业务逻辑。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelSpecDomainService {

    private final ModelSpecGateway modelSpecGateway;

    /**
     * 创建模型规格
     */
    public ModelSpec create(ModelSpec modelSpec) {
        return modelSpecGateway.save(modelSpec);
    }

    /**
     * 更新模型规格
     */
    public ModelSpec update(ModelSpec modelSpec) {
        return modelSpecGateway.save(modelSpec);
    }

    /**
     * 启用模型规格
     */
    public ModelSpec enable(Long id) {
        ModelSpec spec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ProviderException("MODEL_SPEC_NOT_FOUND", "模型规格不存在: " + id));
        spec.setState(ModelSpecState.ACTIVE);
        return modelSpecGateway.save(spec);
    }

    /**
     * 禁用模型规格
     */
    public ModelSpec disable(Long id) {
        ModelSpec spec = modelSpecGateway.findById(id)
                .orElseThrow(() -> new ProviderException("MODEL_SPEC_NOT_FOUND", "模型规格不存在: " + id));
        spec.setState(ModelSpecState.INACTIVE);
        return modelSpecGateway.save(spec);
    }

    /**
     * 根据供应商侧模型 ID 查找
     */
    public Optional<ModelSpec> findByProviderModelId(String providerModelId) {
        return modelSpecGateway.findByProviderModelId(providerModelId);
    }

    /**
     * 查找所有活跃模型规格
     */
    public List<ModelSpec> findAllActive() {
        return modelSpecGateway.findAllActive();
    }

    /**
     * 查找所有模型规格
     */
    public List<ModelSpec> findAll() {
        return modelSpecGateway.findAll();
    }
}