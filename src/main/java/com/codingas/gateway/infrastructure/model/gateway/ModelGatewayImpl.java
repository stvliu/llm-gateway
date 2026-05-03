package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.infrastructure.model.gateway.database.ModelRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ModelDo;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模型网关 JPA 实现
 *
 * <p>实现 ModelGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelGatewayImpl implements ModelGateway {

    private final ModelRepository modelRepository;

    @Override
    public Model save(Model model) {
        ModelDo doEntity = toDo(model);
        ModelDo saved = modelRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<Model> findById(Long id) {
        return modelRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Model> findByModelCode(String modelCode) {
        return modelRepository.findByModelCode(modelCode).map(this::toEntity);
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Model> findAllActive() {
        return modelRepository.findByStatus(ModelDo.ModelStatus.ACTIVE).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Model> findByProviderId(Long providerId) {
        return modelRepository.findByProviderId(providerId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return modelRepository.count();
    }

    @Override
    public void delete(Model model) {
        modelRepository.delete(toDo(model));
    }

    @Override
    public boolean existsByModelCode(String modelCode) {
        return modelRepository.existsByModelCode(modelCode);
    }

    /**
     * DO 转 Entity
     */
    private Model toEntity(ModelDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        Model entity = new Model();
        entity.setId(doEntity.getId());
        entity.setModelCode(doEntity.getModelCode());
        entity.setProviderModelId(doEntity.getProviderModelId());
        entity.setDisplayName(doEntity.getDisplayName());
        entity.setContextWindow(doEntity.getContextWindow());
        entity.setInputPrice(doEntity.getInputPrice());
        entity.setOutputPrice(doEntity.getOutputPrice());
        entity.setCapabilities(doEntity.getCapabilities());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getStatus() != null) {
            entity.setStatus(Model.ModelStatus.valueOf(doEntity.getStatus().name()));
        }
        // Provider 关联转换
        if (doEntity.getProvider() != null) {
            Provider provider = new Provider();
            provider.setId(doEntity.getProvider().getId());
            provider.setProviderCode(doEntity.getProvider().getProviderCode());
            provider.setProviderName(doEntity.getProvider().getProviderName());
            if (doEntity.getProvider().getProviderType() != null) {
                provider.setProviderType(com.codingas.gateway.common.enums.ProviderType.valueOf(
                        doEntity.getProvider().getProviderType().name()));
            }
            provider.setBaseUrl(doEntity.getProvider().getBaseUrl());
            provider.setPriority(doEntity.getProvider().getPriority());
            if (doEntity.getProvider().getStatus() != null) {
                provider.setStatus(Provider.ProviderStatus.valueOf(
                        doEntity.getProvider().getStatus().name()));
            }
            entity.setProvider(provider);
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private ModelDo toDo(Model entity) {
        if (entity == null) {
            return null;
        }
        ModelDo doEntity = new ModelDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setModelCode(entity.getModelCode());
        doEntity.setProviderModelId(entity.getProviderModelId());
        doEntity.setDisplayName(entity.getDisplayName());
        doEntity.setContextWindow(entity.getContextWindow());
        doEntity.setInputPrice(entity.getInputPrice());
        doEntity.setOutputPrice(entity.getOutputPrice());
        doEntity.setCapabilities(entity.getCapabilities());
        doEntity.setDeletedAt(entity.getDeletedAt());
        // 枚举转换
        if (entity.getStatus() != null) {
            doEntity.setStatus(ModelDo.ModelStatus.valueOf(entity.getStatus().name()));
        }
        // Provider 关联 - 只需要设置 ID
        if (entity.getProvider() != null && entity.getProvider().getId() != null) {
            ProviderDo providerDo = new ProviderDo();
            providerDo.setId(entity.getProvider().getId());
            doEntity.setProvider(providerDo);
        }
        return doEntity;
    }
}
