package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelSpecCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelSpecCatalogGateway;
import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ModelSpecCatalogDo;
import com.codingas.gateway.infrastructure.supply.catalog.database.repository.ModelSpecCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格目录持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelSpecCatalogGatewayImpl implements ModelSpecCatalogGateway {

    private final ModelSpecCatalogRepository repository;

    @Override
    public ModelSpecCatalog save(ModelSpecCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ModelSpecCatalog> findByProviderModelId(String providerModelId) {
        return repository.findByProviderModelId(providerModelId).map(this::toEntity);
    }

    @Override
    public boolean existsByProviderModelId(String providerModelId) {
        return repository.existsByProviderModelId(providerModelId);
    }

    @Override
    public List<ModelSpecCatalog> findBySource(CatalogSource source) {
        return repository.findBySource(source.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpecCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpecCatalog> findByKeyword(String keyword) {
        return repository.findByProviderModelIdContainingOrDisplayNameContaining(keyword, keyword)
            .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpecCatalog> findByCapability(String capability) {
        return repository.findByCapability(capability).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelSpecCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activeProviderModelIds) {
        return findBySource(source).stream()
            .filter(entry -> !activeProviderModelIds.contains(entry.getProviderModelId()))
            .toList();
    }

    private ModelSpecCatalog toEntity(ModelSpecCatalogDo doObj) {
        var entity = new ModelSpecCatalog();
        entity.setId(doObj.getId());
        entity.setProviderModelId(doObj.getProviderModelId());
        entity.setDisplayName(doObj.getDisplayName());
        entity.setModelFamily(doObj.getModelFamily());
        entity.setContextWindow(doObj.getContextWindow());
        entity.setMaxInputTokens(doObj.getMaxInputTokens());
        entity.setMaxOutputTokens(doObj.getMaxOutputTokens());
        entity.setKnowledgeCutoff(doObj.getKnowledgeCutoff());
        entity.setCapabilities(doObj.getCapabilities());
        entity.setModalities(doObj.getModalities());
        entity.setSource(CatalogSource.valueOf(doObj.getSource()));
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setState(CatalogState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelSpecCatalogDo toDo(ModelSpecCatalog entity) {
        var doObj = new ModelSpecCatalogDo();
        doObj.setId(entity.getId());
        doObj.setProviderModelId(entity.getProviderModelId());
        doObj.setDisplayName(entity.getDisplayName());
        doObj.setModelFamily(entity.getModelFamily());
        doObj.setContextWindow(entity.getContextWindow());
        doObj.setMaxInputTokens(entity.getMaxInputTokens());
        doObj.setMaxOutputTokens(entity.getMaxOutputTokens());
        doObj.setKnowledgeCutoff(entity.getKnowledgeCutoff());
        doObj.setCapabilities(entity.getCapabilities());
        doObj.setModalities(entity.getModalities());
        doObj.setSource(entity.getSource() != null ? entity.getSource().name() : CatalogSource.BUILTIN.name());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setState(entity.getState() != null ? entity.getState().name() : CatalogState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
