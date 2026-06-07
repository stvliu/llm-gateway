package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ProviderCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.ProviderCatalogGateway;
import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ProviderCatalogDo;
import com.codingas.gateway.infrastructure.supply.catalog.database.repository.ProviderCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 供应商目录持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProviderCatalogGatewayImpl implements ProviderCatalogGateway {

    private final ProviderCatalogRepository repository;

    @Override
    public ProviderCatalog save(ProviderCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProviderCatalog> findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode).map(this::toEntity);
    }

    @Override
    public boolean existsByProviderCode(String providerCode) {
        return repository.existsByProviderCode(providerCode);
    }

    @Override
    public List<ProviderCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ProviderCatalog> findByKeyword(String keyword) {
        return repository.findByProviderCodeContainingOrProviderNameContaining(keyword, keyword)
            .stream().map(this::toEntity).toList();
    }

    private ProviderCatalog toEntity(ProviderCatalogDo doObj) {
        var entity = new ProviderCatalog();
        entity.setId(doObj.getId());
        entity.setProviderCode(doObj.getProviderCode());
        entity.setProviderName(doObj.getProviderName());
        entity.setLogoUrl(doObj.getLogoUrl());
        entity.setWebsiteUrl(doObj.getWebsiteUrl());
        entity.setDescription(doObj.getDescription());
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setState(CatalogState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ProviderCatalogDo toDo(ProviderCatalog entity) {
        var doObj = new ProviderCatalogDo();
        doObj.setId(entity.getId());
        doObj.setProviderCode(entity.getProviderCode());
        doObj.setProviderName(entity.getProviderName());
        doObj.setLogoUrl(entity.getLogoUrl());
        doObj.setWebsiteUrl(entity.getWebsiteUrl());
        doObj.setDescription(entity.getDescription());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setState(entity.getState() != null ? entity.getState().name() : CatalogState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
