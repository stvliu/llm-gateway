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
package com.codingas.gateway.iamdata.gateway;

import com.codingas.gateway.iam.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.apikey.UserApiKeyGateway;
import com.codingas.gateway.iamdata.dataobject.UserApiKeyDo;
import com.codingas.gateway.iamdata.repository.UserApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    public List<UserApiKey> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<UserApiKey> findByApplicationId(Long applicationId) {
        return repository.findByApplicationId(applicationId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<UserApiKey> findAllNonDeleted() {
        return repository.findAllNonDeleted().stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public Optional<UserApiKey> findByKeyPrefix(String keyPrefix) {
        return repository.findByKeyPrefix(keyPrefix).map(this::toEntity);
    }

    @Override
    @Transactional
    public UserApiKey save(UserApiKey userApiKey) {
        UserApiKeyDo dataObject = toDataObject(userApiKey);
        if (userApiKey.getId() == null) {
            dataObject.setCreatedAt(Instant.now());
            dataObject.setDeleted(false);

            // 创建时：从明文计算哈希和密文
            String plainKey = userApiKey.getKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                dataObject.setKeyHash(encryptionService.hashKey(plainKey));
                dataObject.setKeyEncrypted(encryptionService.encrypt(plainKey));
                // 自动生成 keyPrefix（取前 10 位）
                if (dataObject.getKeyPrefix() == null) {
                    dataObject.setKeyPrefix(plainKey.substring(0, Math.min(10, plainKey.length())));
                }
            }
        } else {
            // 更新时：保留已有的 hash 和 encrypted
            repository.findById(userApiKey.getId()).ifPresent(existing -> {
                dataObject.setKeyHash(existing.getKeyHash());
                dataObject.setKeyEncrypted(existing.getKeyEncrypted());
            });
        }
        dataObject.setUpdatedAt(Instant.now());
        UserApiKeyDo saved = repository.save(dataObject);

        return toEntity(saved);
    }

    @Override
    @Transactional
    public void delete(UserApiKey userApiKey) {
        repository.findById(userApiKey.getId()).ifPresent(existing -> {
            existing.setDeleted(true);
            existing.setUpdatedAt(Instant.now());
            repository.save(existing);
        });
    }

    private UserApiKey toEntity(UserApiKeyDo dataObject) {
        UserApiKey entity = new UserApiKey();
        entity.setId(dataObject.getId());
        entity.setUserId(dataObject.getUserId());
        entity.setApplicationId(dataObject.getApplicationId());
        entity.setKeyHash(dataObject.getKeyHash());
        entity.setKeyPrefix(dataObject.getKeyPrefix());
        entity.setName(dataObject.getName());
        entity.setDeleted(dataObject.isDeleted());
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
        dataObject.setUserId(entity.getUserId());
        dataObject.setApplicationId(entity.getApplicationId());
        dataObject.setKeyPrefix(entity.getKeyPrefix());
        dataObject.setName(entity.getName());
        dataObject.setDeleted(entity.isDeleted());
        dataObject.setCreatedAt(entity.getCreatedAt());
        dataObject.setUpdatedAt(entity.getUpdatedAt());
        return dataObject;
    }
}