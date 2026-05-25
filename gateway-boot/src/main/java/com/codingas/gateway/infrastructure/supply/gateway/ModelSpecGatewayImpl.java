package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelSpecDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ModelSpecRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelSpecGatewayImpl implements ModelSpecGateway {

    private final ModelSpecRepository modelSpecRepository;

    @Override
    public ModelSpec save(ModelSpec modelSpec) {
        ModelSpecDo doObj = toDo(modelSpec);
        ModelSpecDo saved = modelSpecRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ModelSpec> findById(Long id) {
        return modelSpecRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<ModelSpec> findByProviderModelId(String providerModelId) {
        return modelSpecRepository.findByProviderModelId(providerModelId).map(this::toEntity);
    }

    @Override
    public List<ModelSpec> findActiveByProviderModelId(String providerModelId) {
        return List.of();
    }

    @Override
    public List<ModelSpec> findAll() {
        return modelSpecRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpec> findAllActive() {
        return modelSpecRepository.findByState(ModelSpecState.ACTIVE.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpec> findByProviderId(Long providerId) {
        return modelSpecRepository.findByProviderId(providerId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<ModelSpec> findByIds(List<Long> ids) {
        return modelSpecRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public long count() {
        return modelSpecRepository.count();
    }

    @Override
    public void delete(ModelSpec modelSpec) {
        modelSpecRepository.deleteById(modelSpec.getId());
    }

    private ModelSpec toEntity(ModelSpecDo doObj) {
        ModelSpec entity = new ModelSpec();
        entity.setId(doObj.getId());
        // TODO: providerId 已从 ModelSpec 移除，此处不再映射
        entity.setProviderModelId(doObj.getProviderModelId());
        entity.setDisplayName(doObj.getDisplayName());
        entity.setModelFamily(doObj.getModelFamily());
        entity.setContextWindow(doObj.getContextWindow());
        entity.setMaxInputTokens(doObj.getMaxInputTokens());
        entity.setMaxOutputTokens(doObj.getMaxOutputTokens());
        entity.setCapabilities(doObj.getCapabilities());
        entity.setModalities(doObj.getModalities());
        entity.setState(ModelSpecState.valueOf(doObj.getState()));
        entity.setPriority(doObj.getPriority());
        entity.setWeight(doObj.getWeight());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelSpecDo toDo(ModelSpec entity) {
        ModelSpecDo doObj = new ModelSpecDo();
        doObj.setId(entity.getId());
        // TODO: providerId 已从 ModelSpec 移除，数据库字段暂保留但不再从 entity 映射
        doObj.setProviderModelId(entity.getProviderModelId());
        doObj.setDisplayName(entity.getDisplayName());
        doObj.setModelFamily(entity.getModelFamily());
        doObj.setContextWindow(entity.getContextWindow());
        doObj.setMaxInputTokens(entity.getMaxInputTokens());
        doObj.setMaxOutputTokens(entity.getMaxOutputTokens());
        doObj.setCapabilities(entity.getCapabilities());
        doObj.setModalities(entity.getModalities());
        doObj.setState(entity.getState() != null ? entity.getState().name() : ModelSpecState.ACTIVE.name());
        doObj.setPriority(entity.getPriority());
        doObj.setWeight(entity.getWeight());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}