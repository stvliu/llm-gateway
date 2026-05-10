package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 提供商网关 JPA 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderGatewayImpl implements ProviderGateway {

    private final ProviderRepository providerRepository;

    @Override
    public Provider save(Provider provider) {
        ProviderDo doEntity = toDo(provider);
        ProviderDo saved = providerRepository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<Provider> findById(Long id) {
        return providerRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<Provider> findAll() {
        return providerRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Provider> findAllActive() {
        return providerRepository.findByEnabledTrue().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return providerRepository.count();
    }

    @Override
    public void delete(Provider provider) {
        providerRepository.delete(toDo(provider));
    }

    /**
     * DO 转 Entity
     */
    private Provider toEntity(ProviderDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        Provider entity = new Provider();
        entity.setId(doEntity.getId());
        entity.setName(doEntity.getName());
        entity.setBaseUrl(doEntity.getBaseUrl());
        entity.setWebsiteUrl(doEntity.getWebsiteUrl());
        entity.setApiDocUrl(doEntity.getApiDocUrl());
        entity.setPriority(doEntity.getPriority());
        entity.setState(doEntity.getState());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        if (doEntity.getType() != null) {
            entity.setType(doEntity.getType());
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private ProviderDo toDo(Provider entity) {
        if (entity == null) {
            return null;
        }
        ProviderDo doEntity = new ProviderDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setName(entity.getName());
        doEntity.setBaseUrl(entity.getBaseUrl());
        doEntity.setWebsiteUrl(entity.getWebsiteUrl());
        doEntity.setApiDocUrl(entity.getApiDocUrl());
        doEntity.setPriority(entity.getPriority());
        doEntity.setState(entity.getState());
        if (entity.getType() != null) {
            doEntity.setType(entity.getType());
        }
        return doEntity;
    }
}