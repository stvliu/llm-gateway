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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 渠道凭证应用服务实现
 *
 * <p>加解密由基础设施层（GatewayImpl）处理，Application 层只传递明文 Key。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelCredentialServiceImpl implements ChannelCredentialService {

    private final ChannelCredentialRepository channelCredentialRepository;

    @Override
    @Transactional
    public ChannelCredential create(ChannelCredentialCreateCommand command) {
        String plainKey = command.apiKey();
        String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(command.channelId());
        credential.setApiKeyPlain(plainKey);
        credential.setApiKeyPrefix(keyPrefix);
        credential.setName(command.description());
        credential.setWeight(command.weight());
        credential.setPriority(command.priority());

        // GatewayImpl 内部处理加密和哈希
        ChannelCredential saved = channelCredentialRepository.save(credential);
        log.info("Created ChannelCredential: id={}, channelId={}", saved.getId(), saved.getChannelId());

        return saved;
    }

    @Override
    public List<ChannelCredential> listByChannelId(Long channelId) {
        return channelCredentialRepository.findByChannelId(channelId);
    }

    @Override
    public ChannelCredential getById(Long channelId, Long id) {
        return findAndValidateOwnership(channelId, id);
    }

    @Override
    public ChannelCredential getDetailById(Long channelId, Long id) {
        return findAndValidateOwnership(channelId, id);
    }

    @Override
    @Transactional
    public ChannelCredential update(ChannelCredentialUpdateCommand command) {
        ChannelCredential credential = findAndValidateOwnership(command.channelId(), command.id());

        if (command.weight() != null) {
            credential.setWeight(command.weight());
        }
        if (command.priority() != null) {
            credential.setPriority(command.priority());
        }
        // 替换 API Key
        if (command.apiKey() != null && !command.apiKey().isBlank()) {
            String newKey = command.apiKey().trim();
            String keyPrefix = newKey.substring(0, Math.min(8, newKey.length()));
            credential.setApiKeyPlain(newKey);
            credential.setApiKeyPrefix(keyPrefix);
        }

        ChannelCredential saved = channelCredentialRepository.save(credential);
        log.info("Updated ChannelCredential: id={}", saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public void delete(Long channelId, Long id) {
        findAndValidateOwnership(channelId, id);
        channelCredentialRepository.deleteById(id);
        log.info("Deleted ChannelCredential: id={}", id);
    }

    @Override
    public ApiKeyTestResult testApiKey(Long channelId, Long id) {
        // 验证归属关系
        ChannelCredential credential = findAndValidateOwnership(channelId, id);

        // TODO: 实现真实的 API Key 测试逻辑
        // 1. 获取 API Key 明文
        // 2. 获取渠道端点配置
        // 3. 发送测试请求
        // 4. 返回测试结果

        log.info("Testing ChannelCredential: id={}, channelId={}", id, channelId);

        return ApiKeyTestResult.builder()
                .success(true)
                .latency(100L)
                .modelName("gpt-4o")
                .responsePreview("Hello! How can I assist you today?")
                .testedAt(Instant.now())
                .build();
    }

    /**
     * 验证归属关系并返回实体
     */
    private ChannelCredential findAndValidateOwnership(Long channelId, Long id) {
        ChannelCredential credential = channelCredentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelCredential", id));
        if (!credential.getChannelId().equals(channelId)) {
            throw new ResourceNotFoundException("ChannelCredential", id);
        }
        return credential;
    }
}
