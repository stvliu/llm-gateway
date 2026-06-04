package com.codingas.gateway.infrastructure.supply.catalog.gateway;

import com.codingas.gateway.domain.supply.catalog.entity.PlanCatalog;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogSource;
import com.codingas.gateway.domain.supply.catalog.enums.CatalogState;
import com.codingas.gateway.domain.supply.catalog.gateway.PlanCatalogGateway;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.infrastructure.supply.catalog.database.dataobject.PlanCatalogDo;
import com.codingas.gateway.infrastructure.supply.catalog.database.repository.PlanCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 套餐目录持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PlanCatalogGatewayImpl implements PlanCatalogGateway {

    private final PlanCatalogRepository repository;

    @Override
    public PlanCatalog save(PlanCatalog catalog) {
        var doEntity = toDo(catalog);
        var saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<PlanCatalog> findByPlanCode(String planCode) {
        return repository.findByPlanCode(planCode).map(this::toEntity);
    }

    @Override
    public boolean existsByPlanCode(String planCode) {
        return repository.existsByPlanCode(planCode);
    }

    @Override
    public List<PlanCatalog> findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanCatalog> findBySource(CatalogSource source) {
        return repository.findBySource(source.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanCatalog> findBySourceExcludingKeys(CatalogSource source, List<String> activePlanCodes) {
        return findBySource(source).stream()
            .filter(entry -> !activePlanCodes.contains(entry.getPlanCode()))
            .toList();
    }

    private PlanCatalog toEntity(PlanCatalogDo doObj) {
        var entity = new PlanCatalog();
        entity.setId(doObj.getId());
        entity.setPlanCode(doObj.getPlanCode());
        entity.setProviderCode(doObj.getProviderCode());
        entity.setPlanName(doObj.getPlanName());
        entity.setBillingMode(BillingMode.resolve(doObj.getBillingMode()));
        entity.setEndpoints(doObj.getEndpoints());
        entity.setPricing(doObj.getPricing());
        entity.setDescription(doObj.getDescription());
        entity.setSource(CatalogSource.valueOf(doObj.getSource()));
        entity.setSyncedAt(doObj.getSyncedAt());
        entity.setState(CatalogState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private PlanCatalogDo toDo(PlanCatalog entity) {
        var doObj = new PlanCatalogDo();
        doObj.setId(entity.getId());
        doObj.setPlanCode(entity.getPlanCode());
        doObj.setProviderCode(entity.getProviderCode());
        doObj.setPlanName(entity.getPlanName());
        doObj.setBillingMode(entity.getBillingMode() != null ? entity.getBillingMode().name() : BillingMode.PAY_AS_YOU_GO.name());
        doObj.setEndpoints(entity.getEndpoints());
        doObj.setPricing(entity.getPricing());
        doObj.setDescription(entity.getDescription());
        doObj.setSource(entity.getSource() != null ? entity.getSource().name() : CatalogSource.BUILTIN.name());
        doObj.setSyncedAt(entity.getSyncedAt());
        doObj.setState(entity.getState() != null ? entity.getState().name() : CatalogState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
