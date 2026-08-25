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
package com.codingas.gateway.providerdata.channel;

import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.encryption.CredentialEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 渠道凭证持久化实现
 *
 * <p>加解密在基础设施层处理：save() 时加密明文 Key，toEntity() 时解密返回明文。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JpaChannelCredentialRepository implements ChannelCredentialRepository {

    private final ChannelCredentialJpaRepository credentialRepository;
    private final CredentialEncryptor encryptor;

    @Override
    public ChannelCredential save(ChannelCredential credential) {
        ChannelCredentialDo doObj = toDo(credential);

        if (credential.getId() == null) {
            // 创建时：从明文计算密文
            String plainKey = credential.getApiKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                doObj.setApiKeyEncrypted(encryptor.encrypt(plainKey));
                // 自动生成 apiKeyPrefix（取前 8 位）
                if (doObj.getApiKeyPrefix() == null || doObj.getApiKeyPrefix().isBlank()) {
                    doObj.setApiKeyPrefix(plainKey.substring(0, Math.min(8, plainKey.length())));
                }
            }
        } else {
            // 更新时：如果提供了新的明文 Key，重新加密；否则保留已有的密文
            String plainKey = credential.getApiKeyPlain();
            if (plainKey != null && !plainKey.isBlank()) {
                doObj.setApiKeyEncrypted(encryptor.encrypt(plainKey));
            } else {
                credentialRepository.findById(credential.getId()).ifPresent(existing -> {
                    doObj.setApiKeyEncrypted(existing.getApiKeyEncrypted());
                    doObj.setApiKeyPlain(existing.getApiKeyPlain());
                });
            }
        }

        ChannelCredentialDo saved = credentialRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ChannelCredential> findById(Long id) {
        return credentialRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ChannelCredential> findByChannelId(Long channelId) {
        return credentialRepository.findByChannelId(channelId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelCredential> findActiveByChannelId(Long channelId) {
        return credentialRepository.findByChannelId(channelId)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ChannelCredential> findDefaultByChannelId(Long channelId) {
        return findActiveByChannelId(channelId).stream().findFirst();
    }

    @Override
    public void deleteById(Long id) {
        credentialRepository.deleteById(id);
    }

    private ChannelCredential toEntity(ChannelCredentialDo doObj) {
        ChannelCredential entity = new ChannelCredential();
        entity.setId(doObj.getId());
        entity.setChannelId(doObj.getChannelId());
        entity.setName(doObj.getName());
        entity.setApiKeyPrefix(doObj.getApiKeyPrefix());
        entity.setKeyAlias(doObj.getKeyAlias());
        entity.setWeight(doObj.getWeight());
        entity.setPriority(doObj.getPriority());
        entity.setLastUsedAt(doObj.getLastUsedAt());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());

        // 解密返回明文 Key
        if (doObj.getApiKeyEncrypted() != null && !doObj.getApiKeyEncrypted().isBlank()) {
            try {
                entity.setApiKeyPlain(encryptor.decrypt(doObj.getApiKeyEncrypted()));
            } catch (Exception e) {
                log.warn("解密渠道凭证失败: id={}, error={}", doObj.getId(), e.getMessage());
                entity.setApiKeyPlain(null);
            }
        }

        return entity;
    }

    private ChannelCredentialDo toDo(ChannelCredential entity) {
        ChannelCredentialDo doObj = new ChannelCredentialDo();
        doObj.setId(entity.getId());
        doObj.setChannelId(entity.getChannelId());
        doObj.setName(entity.getName());
        doObj.setApiKeyPrefix(entity.getApiKeyPrefix());
        doObj.setKeyAlias(entity.getKeyAlias());
        doObj.setWeight(entity.getWeight());
        doObj.setPriority(entity.getPriority());
        doObj.setLastUsedAt(entity.getLastUsedAt());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
