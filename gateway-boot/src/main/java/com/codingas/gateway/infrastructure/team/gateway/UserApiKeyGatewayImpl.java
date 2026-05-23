package com.codingas.gateway.infrastructure.team.gateway;

import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserApiKeyDo;
import com.codingas.gateway.infrastructure.team.gateway.database.repository.UserApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key 领域网关实现
 *
 * <p>加解密在基础设施层处理：save() 时加密明文 Key，toEntity() 时解密返回明文。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserApiKeyGatewayImpl implements UserApiKeyGateway {

    private final UserApiKeyRepository repository;
    private final ApiKeyEncryptionDomainService encryptionService;

    @Override
    public Optional<UserApiKey> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public List<UserApiKey> findByTeamId(Long teamId) {
        return repository.findByTeamId(teamId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<UserApiKey> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public Optional<UserApiKey> findByKeyPrefix(String keyPrefix) {
        return repository.findByKeyPrefix(keyPrefix).map(this::toEntity);
    }

    @Override
    public UserApiKey save(UserApiKey userApiKey) {
        UserApiKeyDo dataObject = toDataObject(userApiKey);
        if (userApiKey.getId() == null) {
            dataObject.setCreatedAt(Instant.now());

            // 创建时：从明文计算哈希和密文
            String plainKey = userApiKey.getKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                dataObject.setKeyHash(encryptionService.hashKey(plainKey));
                dataObject.setKeyEncrypted(encryptionService.encrypt(plainKey));
                dataObject.setKeyPrefix(plainKey.substring(0, Math.min(8, plainKey.length())));
            }
        }
        dataObject.setUpdatedAt(Instant.now());
        UserApiKeyDo saved = repository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public long countByTeamId(Long teamId) {
        return repository.countByTeamId(teamId);
    }

    @Override
    public List<Long> findIdsByProductId(Long productId) {
        return repository.findIdsByProductId(productId);
    }

    private UserApiKey toEntity(UserApiKeyDo dataObject) {
        UserApiKey entity = new UserApiKey();
        entity.setId(dataObject.getId());
        entity.setTeamId(dataObject.getTeamId());
        entity.setUserId(dataObject.getUserId());
        entity.setProductIds(new ArrayList<>(dataObject.getProductIds()));
        entity.setKeyHash(dataObject.getKeyHash());
        entity.setKeyPrefix(dataObject.getKeyPrefix());
        entity.setName(dataObject.getName());
        entity.setModels(parseModels(dataObject.getModels()));
        entity.setQuotaLimit(dataObject.getQuotaLimit());
        entity.setState(dataObject.getState());
        entity.setCreatedAt(dataObject.getCreatedAt());
        entity.setUpdatedAt(dataObject.getUpdatedAt());

        // 解密返回明文 Key
        if (dataObject.getKeyEncrypted() != null && !dataObject.getKeyEncrypted().isBlank()) {
            try {
                entity.setKeyPlain(encryptionService.decrypt(dataObject.getKeyEncrypted()));
            } catch (Exception e) {
                log.warn("Failed to decrypt UserApiKey: id={}, error={}", dataObject.getId(), e.getMessage());
                entity.setKeyPlain(null);
            }
        }

        return entity;
    }

    private UserApiKeyDo toDataObject(UserApiKey entity) {
        UserApiKeyDo dataObject = new UserApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setTeamId(entity.getTeamId());
        dataObject.setUserId(entity.getUserId());
        dataObject.setProductIds(entity.getProductIds() != null
                ? new HashSet<>(entity.getProductIds())
                : new HashSet<>());
        dataObject.setKeyPrefix(entity.getKeyPrefix());
        dataObject.setName(entity.getName());
        dataObject.setModels(formatModels(entity.getModels()));
        dataObject.setQuotaLimit(entity.getQuotaLimit());
        dataObject.setState(entity.getState() != null ? entity.getState() : UserApiKeyState.ACTIVE);
        dataObject.setCreatedAt(entity.getCreatedAt());
        dataObject.setUpdatedAt(entity.getUpdatedAt());

        // 更新时：保留已有的 hash 和 encrypted
        if (entity.getId() != null) {
            repository.findById(entity.getId()).ifPresent(existing -> {
                dataObject.setKeyHash(existing.getKeyHash());
                dataObject.setKeyEncrypted(existing.getKeyEncrypted());
            });
        }

        return dataObject;
    }

    private List<String> parseModels(String modelsStr) {
        if (modelsStr == null || modelsStr.isBlank()) {
            return null;
        }
        return List.of(modelsStr.split(","));
    }

    private String formatModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return null;
        }
        return String.join(",", models);
    }
}