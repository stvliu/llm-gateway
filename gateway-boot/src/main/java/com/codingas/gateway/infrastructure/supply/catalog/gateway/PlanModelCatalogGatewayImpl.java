package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanModelCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanModelCatalogGateway;
import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.PlanModelCatalogDo;
import com.codingas.gateway.infrastructure.supply.catalog.database.repository.PlanModelCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 套餐-模型关联目录持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanModelCatalogGatewayImpl implements PlanModelCatalogGateway {

    private final PlanModelCatalogRepository repository;

    @Override
    public PlanModelCatalog save(PlanModelCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<PlanModelCatalog> findByPlanCodeAndProviderModelId(String planCode, String providerModelId) {
        return repository.findByPlanCodeAndProviderModelId(planCode, providerModelId).map(this::toEntity);
    }

    @Override
    public List<PlanModelCatalog> findByPlanCode(String planCode) {
        return repository.findByPlanCode(planCode).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanModelCatalog> findByProviderModelId(String providerModelId) {
        return repository.findByProviderModelId(providerModelId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanModelCatalog> findBySource(CatalogSource source) {
        return repository.findBySource(source.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanModelCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanModelCatalog> findBySourceExcludingKeys(CatalogSource source,
                                                             List<String> activePlanCodes,
                                                             List<String> activeProviderModelIds) {
        return findBySource(source).stream()
            .filter(entry -> !(activePlanCodes.contains(entry.getPlanCode())
                && activeProviderModelIds.contains(entry.getProviderModelId())))
            .toList();
    }

    private PlanModelCatalog toEntity(PlanModelCatalogDo doObj) {
        var entity = new PlanModelCatalog();
        entity.setId(doObj.getId());
        entity.setPlanCode(doObj.getPlanCode());
        entity.setProviderModelId(doObj.getProviderModelId());
        entity.setSource(CatalogSource.valueOf(doObj.getSource()));
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setState(CatalogState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private PlanModelCatalogDo toDo(PlanModelCatalog entity) {
        var doObj = new PlanModelCatalogDo();
        doObj.setId(entity.getId());
        doObj.setPlanCode(entity.getPlanCode());
        doObj.setProviderModelId(entity.getProviderModelId());
        doObj.setSource(entity.getSource() != null ? entity.getSource().name() : CatalogSource.BUILTIN.name());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setState(entity.getState() != null ? entity.getState().name() : CatalogState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
