package com.codingas.gateway.infrastructure.model.gateway;

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
 *
 * <p>实现 ProviderGateway 接口，负责 DO ↔ Entity 转换。</p>
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
    public Optional<Provider> findByProviderCode(String providerCode) {
        return providerRepository.findByProviderCode(providerCode).map(this::toEntity);
    }

    @Override
    public List<Provider> findAll() {
        return providerRepository.findAll().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Provider> findAllActive() {
        return providerRepository.findByStatus(ProviderDo.ProviderStatus.ACTIVE).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Provider> findByStatus(Provider.ProviderStatus status) {
        return providerRepository.findByStatus(ProviderDo.ProviderStatus.valueOf(status.name())).stream()
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

    @Override
    public boolean existsByProviderCode(String providerCode) {
        return providerRepository.existsByProviderCode(providerCode);
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
        entity.setProviderCode(doEntity.getProviderCode());
        entity.setProviderName(doEntity.getProviderName());
        entity.setBaseUrl(doEntity.getBaseUrl());
        entity.setWebsiteUrl(doEntity.getWebsiteUrl());
        entity.setApiDocUrl(doEntity.getApiDocUrl());
        entity.setPriority(doEntity.getPriority());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getProviderType() != null) {
            entity.setProviderType(com.codingas.gateway.common.enums.ProviderType.valueOf(doEntity.getProviderType().name()));
        }
        if (doEntity.getStatus() != null) {
            entity.setStatus(Provider.ProviderStatus.valueOf(doEntity.getStatus().name()));
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
        doEntity.setProviderCode(entity.getProviderCode());
        doEntity.setProviderName(entity.getProviderName());
        doEntity.setBaseUrl(entity.getBaseUrl());
        doEntity.setWebsiteUrl(entity.getWebsiteUrl());
        doEntity.setApiDocUrl(entity.getApiDocUrl());
        doEntity.setPriority(entity.getPriority());
        doEntity.setDeletedAt(entity.getDeletedAt());
        // 枚举转换
        if (entity.getProviderType() != null) {
            doEntity.setProviderType(entity.getProviderType());
        }
        if (entity.getStatus() != null) {
            doEntity.setStatus(ProviderDo.ProviderStatus.valueOf(entity.getStatus().name()));
        }
        return doEntity;
    }
}