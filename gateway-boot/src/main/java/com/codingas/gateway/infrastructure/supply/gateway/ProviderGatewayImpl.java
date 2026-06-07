package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.ProviderState;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ProviderDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 供应商持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProviderGatewayImpl implements ProviderGateway {

    private final ProviderRepository providerRepository;

    @Override
    public Provider save(Provider provider) {
        ProviderDo doObj = toDo(provider);
        ProviderDo saved = providerRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<Provider> findById(Long id) {
        return providerRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<Provider> findByCode(String code) {
        return providerRepository.findByCode(code).map(this::toEntity);
    }

    @Override
    public Optional<Provider> findByName(String name) {
        return providerRepository.findByName(name).map(this::toEntity);
    }

    @Override
    public List<Provider> findAll() {
        return providerRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public List<Provider> findAllActive() {
        return providerRepository.findByState(ProviderState.ACTIVE.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public long count() {
        return providerRepository.count();
    }

    @Override
    public void delete(Provider provider) {
        providerRepository.deleteById(provider.getId());
    }

    @Override
    public void deleteById(Long id) {
        providerRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return providerRepository.existsByName(name);
    }

    @Override
    public boolean existsByCode(String code) {
        return providerRepository.existsByCode(code);
    }

    @Override
    public List<Provider> findByKeyword(String keyword) {
        return providerRepository.findByCodeContainingOrNameContaining(keyword, keyword)
                .stream().map(this::toEntity).toList();
    }

    private Provider toEntity(ProviderDo doObj) {
        Provider entity = new Provider();
        entity.setId(doObj.getId());
        entity.setCode(doObj.getCode());
        entity.setName(doObj.getName());
        entity.setLogoUrl(doObj.getLogoUrl());
        entity.setWebsiteUrl(doObj.getWebsiteUrl());
        entity.setDescription(doObj.getDescription());
        entity.setState(ProviderState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ProviderDo toDo(Provider entity) {
        ProviderDo doObj = new ProviderDo();
        doObj.setId(entity.getId());
        doObj.setCode(entity.getCode());
        doObj.setName(entity.getName());
        doObj.setLogoUrl(entity.getLogoUrl());
        doObj.setWebsiteUrl(entity.getWebsiteUrl());
        doObj.setDescription(entity.getDescription());
        doObj.setState(entity.getState() != null ? entity.getState().name() : ProviderState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}