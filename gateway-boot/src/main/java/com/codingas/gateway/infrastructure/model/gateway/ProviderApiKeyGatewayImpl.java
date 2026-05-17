package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.application.provider.dto.ProviderKeyStats;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 提供商 API 密钥网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderApiKeyGatewayImpl implements ProviderApiKeyGateway {

    private final ProviderApiKeyRepository repository;
    private final ApiKeyEncryptionDomainService encryptionService;

    @Override
    public Optional<ProviderApiKey> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ProviderApiKey> findByProviderId(Long providerId) {
        return repository.findByProviderId(providerId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProviderApiKey> findByProviderId(Long providerId, Pageable pageable) {
        return repository.findByProviderId(providerId, pageable).map(this::toEntity);
    }

    @Override
    public Page<ProviderApiKey> findByProviderIdAndState(Long providerId, ProviderApiKeyState state, Pageable pageable) {
        return repository.findByProviderIdAndState(providerId, state, pageable).map(this::toEntity);
    }

    @Override
    public Page<ProviderApiKey> findByProviderIdAndKeyword(Long providerId, String keyword, Pageable pageable) {
        return repository.findByProviderIdAndKeyword(providerId, keyword, pageable).map(this::toEntity);
    }

    @Override
    public Page<ProviderApiKey> findByProviderIdAndStateAndKeyword(Long providerId, ProviderApiKeyState state, String keyword, Pageable pageable) {
        return repository.findByProviderIdAndStateAndKeyword(providerId, state, keyword, pageable).map(this::toEntity);
    }

    @Override
    public List<ProviderApiKey> findActiveKeysByProviderId(Long providerId) {
        return repository.findByProviderIdAndState(providerId, ProviderApiKeyState.ACTIVE).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProviderApiKey> findDefaultKeyByProviderId(Long providerId) {
        return repository.findByProviderIdAndIsDefaultTrue(providerId).map(this::toEntity);
    }

    @Override
    public long countByProviderId(Long providerId) {
        return repository.countByProviderId(providerId);
    }

    @Override
    @Transactional
    public ProviderApiKey save(ProviderApiKey providerApiKey) {
        ProviderApiKeyDo doEntity = toDo(providerApiKey);
        ProviderApiKeyDo saved = repository.save(doEntity);

        // 如果设置为默认 Key，清除其他 Key 的默认标记
        if (Boolean.TRUE.equals(providerApiKey.getIsDefault()) && providerApiKey.getId() != null) {
            repository.clearDefaultFlagForOtherKeys(providerApiKey.getProviderId(), providerApiKey.getId());
        }

        return toEntity(saved);
    }

    @Override
    @Transactional
    public void updateState(Long id, ProviderApiKeyState state) {
        repository.updateState(id, state);
    }

    @Override
    @Transactional
    public void updateLastUsedAt(Long id, Instant lastUsedAt) {
        repository.updateLastUsedAt(id, lastUsedAt);
    }

    @Override
    @Transactional
    public void clearDefaultFlagForOtherKeys(Long providerId, Long excludeId) {
        repository.clearDefaultFlagForOtherKeys(providerId, excludeId);
    }

    @Override
    public Map<Long, ProviderKeyStats> getKeyStatsByProviderIds(List<Long> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ProviderKeyStats> result = new HashMap<>();

        for (Long providerId : providerIds) {
            List<ProviderApiKeyDo> keys = repository.findByProviderId(providerId);
            int totalCount = keys.size();
            int activeCount = (int) keys.stream()
                .filter(k -> k.getState() == ProviderApiKeyState.ACTIVE)
                .count();
            result.put(providerId, new ProviderKeyStats(providerId, totalCount, activeCount));
        }

        return result;
    }

    @Override
    @Transactional
    public void delete(ProviderApiKey providerApiKey) {
        if (providerApiKey != null && providerApiKey.getId() != null) {
            repository.deleteById(providerApiKey.getId());
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id != null) {
            repository.deleteById(id);
        }
    }

    private ProviderApiKey toEntity(ProviderApiKeyDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        ProviderApiKey entity = new ProviderApiKey();
        entity.setId(doEntity.getId());
        entity.setProviderId(doEntity.getProviderId());
        entity.setKeyName(doEntity.getKeyName());
        entity.setApiKey(encryptionService.decrypt(doEntity.getApiKey()));
        entity.setPriority(doEntity.getPriority());
        entity.setWeight(doEntity.getWeight());
        entity.setIsDefault(doEntity.getIsDefault());
        entity.setLastUsedAt(doEntity.getLastUsedAt());
        entity.setRpmLimit(doEntity.getRpmLimit());
        entity.setTpmLimit(doEntity.getTpmLimit());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        if (doEntity.getState() != null) {
            entity.setState(doEntity.getState());
        }
        return entity;
    }

    private ProviderApiKeyDo toDo(ProviderApiKey entity) {
        if (entity == null) {
            return null;
        }
        ProviderApiKeyDo doEntity = new ProviderApiKeyDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setProviderId(entity.getProviderId());
        doEntity.setKeyName(entity.getKeyName());
        // 加密存储：明文 apiKey 加密后存入 api_key 字段
        if (entity.getApiKey() != null && !entity.getApiKey().isBlank()) {
            doEntity.setApiKey(encryptionService.encrypt(entity.getApiKey()));
        }
        doEntity.setPriority(entity.getPriority());
        doEntity.setWeight(entity.getWeight());
        doEntity.setIsDefault(entity.getIsDefault());
        doEntity.setLastUsedAt(entity.getLastUsedAt());
        doEntity.setRpmLimit(entity.getRpmLimit());
        doEntity.setTpmLimit(entity.getTpmLimit());
        if (entity.getState() != null) {
            doEntity.setState(entity.getState());
        }
        return doEntity;
    }
}
