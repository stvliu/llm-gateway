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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key Gateway 实现
 *
 * <p>加解密在基础设施层处理：save() 时加密明文 Key，toEntity() 时解密返回明文。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserApiKeyGatewayImpl implements UserApiKeyGateway {

    private final UserApiKeyRepository userApiKeyRepository;
    private final ApiKeyEncryptionDomainService encryptionService;

    @Override
    public UserApiKey save(UserApiKey apiKey) {
        UserApiKeyDo dataObject = toDataObject(apiKey);
        if (apiKey.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());

            // 创建时：从明文计算哈希和密文
            String plainKey = apiKey.getKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                dataObject.setKeyHash(encryptionService.hashKey(plainKey));
                dataObject.setKeyEncrypted(encryptionService.encrypt(plainKey));
                dataObject.setKeyPrefix(plainKey.substring(0, Math.min(8, plainKey.length())));
            }
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        UserApiKeyDo saved = userApiKeyRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<UserApiKey> findById(Long id) {
        return userApiKeyRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<UserApiKey> findByKeyHash(String keyHash) {
        return userApiKeyRepository.findByKeyHash(keyHash).map(this::toEntity);
    }

    @Override
    public List<UserApiKey> findByTeamId(Long teamId) {
        return userApiKeyRepository.findByTeamId(teamId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<UserApiKey> findByProductId(Long productId) {
        return userApiKeyRepository.findByProductId(productId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        userApiKeyRepository.deleteById(id);
    }

    @Override
    public long countByTeamId(Long teamId) {
        return userApiKeyRepository.countByTeamId(teamId);
    }

    private UserApiKey toEntity(UserApiKeyDo dataObject) {
        UserApiKey entity = new UserApiKey();
        entity.setId(dataObject.getId());
        entity.setTeamId(dataObject.getTeamId());
        entity.setOwnerUserId(dataObject.getOwnerUserId());
        entity.setProductId(dataObject.getProductId());
        entity.setKeyPrefix(dataObject.getKeyPrefix());
        entity.setName(dataObject.getName());
        entity.setModels(dataObject.getModels());
        entity.setQuotaLimit(dataObject.getQuotaLimit());
        entity.setState(UserApiKeyState.fromCode(dataObject.getState()));

        // 解密返回明文 Key
        if (dataObject.getKeyEncrypted() != null && !dataObject.getKeyEncrypted().isBlank()) {
            try {
                entity.setKeyPlain(encryptionService.decrypt(dataObject.getKeyEncrypted()));
            } catch (Exception e) {
                log.warn("Failed to decrypt UserApiKey: id={}, error={}", dataObject.getId(), e.getMessage());
                entity.setKeyPlain(null);
            }
        }

        if (dataObject.getCreatedAt() != null) {
            entity.setCreatedAt(dataObject.getCreatedAt().toInstant(ZoneOffset.UTC));
        }
        if (dataObject.getUpdatedAt() != null) {
            entity.setUpdatedAt(dataObject.getUpdatedAt().toInstant(ZoneOffset.UTC));
        }
        return entity;
    }

    private UserApiKeyDo toDataObject(UserApiKey entity) {
        UserApiKeyDo dataObject = new UserApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setTeamId(entity.getTeamId());
        dataObject.setOwnerUserId(entity.getOwnerUserId());
        dataObject.setProductId(entity.getProductId());
        dataObject.setKeyPrefix(entity.getKeyPrefix());
        dataObject.setName(entity.getName());
        dataObject.setModels(entity.getModels());
        dataObject.setQuotaLimit(entity.getQuotaLimit());
        dataObject.setState(entity.getState().getCode());

        // 更新时：保留已有的 hash 和 encrypted
        if (entity.getId() != null) {
            userApiKeyRepository.findById(entity.getId()).ifPresent(existing -> {
                dataObject.setKeyHash(existing.getKeyHash());
                dataObject.setKeyEncrypted(existing.getKeyEncrypted());
            });
        }

        return dataObject;
    }
}