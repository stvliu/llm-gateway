package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.ModelCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.ModelCatalogGateway;
import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.ModelCatalogDo;
import com.codingas.gateway.infrastructure.supply.catalog.database.repository.ModelCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 模型目录持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelCatalogGatewayImpl implements ModelCatalogGateway {

    private final ModelCatalogRepository repository;

    @Override
    public ModelCatalog save(ModelCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ModelCatalog> findByModelName(String modelName) {
        return repository.findByModelName(modelName).map(this::toEntity);
    }

    @Override
    public boolean existsByModelName(String modelName) {
        return repository.existsByModelName(modelName);
    }

    @Override
    public List<ModelCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelCatalog> findByKeyword(String keyword) {
        return repository.findByModelNameContainingOrDisplayNameContaining(keyword, keyword)
            .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ModelCatalog> findByCapability(String capability) {
        return repository.findByCapability(capability).stream().map(this::toEntity).toList();
    }

    private ModelCatalog toEntity(ModelCatalogDo doObj) {
        var entity = new ModelCatalog();
        entity.setId(doObj.getId());
        entity.setModelName(doObj.getModelName());
        entity.setDisplayName(doObj.getDisplayName());
        entity.setModelFamily(doObj.getModelFamily());
        entity.setContextWindow(doObj.getContextWindow());
        entity.setMaxInputTokens(doObj.getMaxInputTokens());
        entity.setMaxOutputTokens(doObj.getMaxOutputTokens());
        entity.setKnowledgeCutoff(doObj.getKnowledgeCutoff());
        entity.setCapabilities(doObj.getCapabilities());
        entity.setModalities(doObj.getModalities());
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setState(CatalogState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ModelCatalogDo toDo(ModelCatalog entity) {
        var doObj = new ModelCatalogDo();
        doObj.setId(entity.getId());
        doObj.setModelName(entity.getModelName());
        doObj.setDisplayName(entity.getDisplayName());
        doObj.setModelFamily(entity.getModelFamily());
        doObj.setContextWindow(entity.getContextWindow());
        doObj.setMaxInputTokens(entity.getMaxInputTokens());
        doObj.setMaxOutputTokens(entity.getMaxOutputTokens());
        doObj.setKnowledgeCutoff(entity.getKnowledgeCutoff());
        doObj.setCapabilities(entity.getCapabilities());
        doObj.setModalities(entity.getModalities());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setState(entity.getState() != null ? entity.getState().name() : CatalogState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}