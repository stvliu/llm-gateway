/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.providerdata.catalog;

import com.codingas.gateway.provider.catalog.PlanCatalog;
import com.codingas.gateway.provider.catalog.PlanCatalogRepository;
import com.codingas.gateway.provider.model.BillingMode;
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
public class JpaPlanCatalogRepository implements PlanCatalogRepository {

    private final PlanCatalogJpaRepository repository;

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
    public List<PlanCatalog> findByProviderCode(String providerCode) {
        return repository.findByProviderCode(providerCode).stream().map(this::toEntity).toList();
    }

    @Override
    public List<PlanCatalog> findAll() {
        return repository.findAll().stream().map(this::toEntity).toList();
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
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
