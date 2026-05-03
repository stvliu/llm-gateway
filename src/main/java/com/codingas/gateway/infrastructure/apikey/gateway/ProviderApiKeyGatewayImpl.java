package com.codingas.gateway.infrastructure.apikey.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.infrastructure.apikey.gateway.database.dataobject.ProviderApiKeyDo;
import com.codingas.gateway.infrastructure.apikey.gateway.database.ProviderApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 提供商 API 密钥网关实现
 *
 * <p>实现 ProviderApiKeyGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderApiKeyGatewayImpl implements ProviderApiKeyGateway {

    private final ProviderApiKeyRepository repository;

    @Override
    public Optional<ProviderApiKey> findByProviderCode(String providerCode) {
        return repository.findByKeyCode(providerCode).map(this::toEntity);
    }

    @Override
    public Optional<ProviderApiKey> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public ProviderApiKey save(ProviderApiKey providerApiKey) {
        ProviderApiKeyDo doEntity = toDo(providerApiKey);
        ProviderApiKeyDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    /**
     * DO 转 Entity
     */
    private ProviderApiKey toEntity(ProviderApiKeyDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        ProviderApiKey entity = new ProviderApiKey();
        entity.setId(doEntity.getId());
        entity.setKeyCode(doEntity.getKeyCode());
        entity.setProviderId(doEntity.getProviderId());
        entity.setKeyName(doEntity.getKeyName());
        entity.setApiKey(doEntity.getApiKey());
        entity.setEncryptedApiKey(doEntity.getEncryptedApiKey());
        entity.setPriority(doEntity.getPriority());
        entity.setLastUsedAt(doEntity.getLastUsedAt());
        entity.setExpiresAt(doEntity.getExpiresAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getStatus() != null) {
            entity.setStatus(ProviderApiKey.ProviderApiKeyStatus.valueOf(doEntity.getStatus().name()));
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private ProviderApiKeyDo toDo(ProviderApiKey entity) {
        if (entity == null) {
            return null;
        }
        ProviderApiKeyDo doEntity = new ProviderApiKeyDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setKeyCode(entity.getKeyCode());
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setKeyName(entity.getKeyName());
        doEntity.setApiKey(entity.getApiKey());
        doEntity.setEncryptedApiKey(entity.getEncryptedApiKey());
        doEntity.setPriority(entity.getPriority());
        doEntity.setLastUsedAt(entity.getLastUsedAt());
        doEntity.setExpiresAt(entity.getExpiresAt());
        // 枚举转换
        if (entity.getStatus() != null) {
            doEntity.setStatus(ProviderApiKeyDo.ProviderApiKeyStatus.valueOf(entity.getStatus().name()));
        }
        return doEntity;
    }
}
