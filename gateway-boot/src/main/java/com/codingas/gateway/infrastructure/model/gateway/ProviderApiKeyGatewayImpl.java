package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyStatus;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyDisabledReason;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public Optional<ProviderApiKey> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    @Deprecated
    public Optional<ProviderApiKey> findByProviderId(Long providerId) {
        return repository.findByProviderId(providerId).map(this::toEntity);
    }

    @Override
    public List<ProviderApiKey> findByChannelId(Long channelId) {
        return repository.findByChannelId(channelId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProviderApiKey> findActiveKeysByChannelId(Long channelId) {
        return repository.findActiveKeysByChannelId(channelId, Instant.now()).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProviderApiKey> findDefaultKeyByChannelId(Long channelId) {
        return repository.findByChannelIdAndIsDefaultTrue(channelId).map(this::toEntity);
    }

    @Override
    public long countByChannelId(Long channelId) {
        return repository.countByChannelId(channelId);
    }

    @Override
    @Transactional
    public ProviderApiKey save(ProviderApiKey providerApiKey) {
        ProviderApiKeyDo doEntity = toDo(providerApiKey);
        ProviderApiKeyDo saved = repository.save(doEntity);

        // 如果设置为默认 Key，清除其他 Key 的默认标记
        if (Boolean.TRUE.equals(providerApiKey.getIsDefault()) && providerApiKey.getId() != null) {
            repository.clearDefaultFlagForOtherKeys(providerApiKey.getChannelId(), providerApiKey.getId());
        }

        return toEntity(saved);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, ProviderApiKeyStatus status, ProviderApiKeyDisabledReason reason) {
        repository.updateStatus(id,
            ProviderApiKeyDo.ProviderApiKeyStatus.valueOf(status.name()),
            reason != null ? ProviderApiKeyDo.ProviderApiKeyDisabledReason.valueOf(reason.name()) : null);
    }

    @Override
    @Transactional
    public void updateLastUsedAt(Long id, Instant lastUsedAt) {
        repository.updateLastUsedAt(id, lastUsedAt);
    }

    @Override
    @Transactional
    public void clearDefaultFlagForOtherKeys(Long channelId, Long excludeId) {
        repository.clearDefaultFlagForOtherKeys(channelId, excludeId);
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
        entity.setProviderId(doEntity.getProviderId());
        entity.setChannelId(doEntity.getChannelId());
        entity.setKeyName(doEntity.getKeyName());
        entity.setApiKey(doEntity.getApiKey());
        entity.setEncryptedApiKey(doEntity.getEncryptedApiKey());
        entity.setPriority(doEntity.getPriority());
        entity.setWeight(doEntity.getWeight());
        entity.setIsDefault(doEntity.getIsDefault());
        entity.setLastUsedAt(doEntity.getLastUsedAt());
        entity.setExpiresAt(doEntity.getExpiresAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        // 枚举转换
        if (doEntity.getStatus() != null) {
            entity.setStatus(ProviderApiKeyStatus.valueOf(doEntity.getStatus().name()));
        }
        if (doEntity.getDisabledReason() != null) {
            entity.setDisabledReason(ProviderApiKeyDisabledReason.valueOf(doEntity.getDisabledReason().name()));
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
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setChannelId(entity.getChannelId());
        doEntity.setKeyName(entity.getKeyName());
        doEntity.setApiKey(entity.getApiKey());
        doEntity.setEncryptedApiKey(entity.getEncryptedApiKey());
        doEntity.setPriority(entity.getPriority());
        doEntity.setWeight(entity.getWeight());
        doEntity.setIsDefault(entity.getIsDefault());
        doEntity.setLastUsedAt(entity.getLastUsedAt());
        doEntity.setExpiresAt(entity.getExpiresAt());
        // 枚举转换
        if (entity.getStatus() != null) {
            doEntity.setStatus(ProviderApiKeyDo.ProviderApiKeyStatus.valueOf(entity.getStatus().name()));
        }
        if (entity.getDisabledReason() != null) {
            doEntity.setDisabledReason(ProviderApiKeyDo.ProviderApiKeyDisabledReason.valueOf(entity.getDisabledReason().name()));
        }
        return doEntity;
    }
}
