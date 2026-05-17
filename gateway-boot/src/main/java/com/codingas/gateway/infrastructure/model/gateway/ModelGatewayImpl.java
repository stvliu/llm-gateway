package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.entity.Model;
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
        return modelRepository.findByIdWithProvider(id).map(this::toEntity);
    }

    @Override
    public Optional<Model> findByProviderModelId(String providerModelId) {
        return modelRepository.findByProviderModelId(providerModelId).map(this::toEntity);
    }

    @Override
    public List<Model> findActiveByProviderModelId(String providerModelId) {
        return modelRepository.findActiveByProviderModelId(providerModelId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Model> findAllByProviderModelId(String providerModelId) {
        return modelRepository.findAllByProviderModelId(providerModelId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Model> findAll() {
        return modelRepository.findAllWithProvider().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Model> findAllActive() {
        return modelRepository.findActive().stream()
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

    /**
     * DO 转 Entity
     */
    private Model toEntity(ModelDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        Model entity = new Model();
        entity.setId(doEntity.getId());
        entity.setProviderModelId(doEntity.getProviderModelId());
        entity.setDisplayName(doEntity.getDisplayName());
        entity.setContextWindow(doEntity.getContextWindow());
        entity.setInputPrice(doEntity.getInputPrice());
        entity.setOutputPrice(doEntity.getOutputPrice());
        entity.setCapabilities(doEntity.getCapabilities());
        entity.setState(doEntity.getState());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 路由字段
        entity.setPriority(doEntity.getPriority());
        entity.setWeight(doEntity.getWeight());
        // Provider 关联 - 使用 ID 引用
        if (doEntity.getProvider() != null) {
            entity.setProviderId(doEntity.getProvider().getId());
            entity.setProviderName(doEntity.getProvider().getName());
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
        doEntity.setProviderModelId(entity.getProviderModelId());
        doEntity.setDisplayName(entity.getDisplayName());
        doEntity.setContextWindow(entity.getContextWindow());
        doEntity.setInputPrice(entity.getInputPrice());
        doEntity.setOutputPrice(entity.getOutputPrice());
        doEntity.setCapabilities(entity.getCapabilities());
        doEntity.setState(entity.getState());
        // 路由字段
        doEntity.setPriority(entity.getPriority());
        doEntity.setWeight(entity.getWeight());
        // Provider 关联 - 只需要设置 ID
        if (entity.getProviderId() != null) {
            ProviderDo providerDo = new ProviderDo();
            providerDo.setId(entity.getProviderId());
            doEntity.setProvider(providerDo);
        }
        return doEntity;
    }
}