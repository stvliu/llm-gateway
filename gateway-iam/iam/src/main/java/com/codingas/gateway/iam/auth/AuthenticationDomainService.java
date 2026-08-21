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
package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.apikey.UserApiKeyGateway;
import com.codingas.gateway.iam.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.iam.valueobject.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 认证领域服务
 *
 * <p>认证流程：通过 keyPrefix 查找 Key，再用 hash 验证完整密钥。</p>
 * <p>不再解密密钥做明文比较，避免密钥在认证流程中暴露。</p>
 */
@Service
public class AuthenticationDomainService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationDomainService.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    public AuthenticationDomainService(UserApiKeyGateway userApiKeyGateway,
                                       ApiKeyEncryptionDomainService encryptionService) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.encryptionService = encryptionService;
    }

    /**
     * 认证用户 API Key
     *
     * @param apiKey 明文 API Key
     * @return 认证结果
     * @throws AuthenticationFailedException 认证失败
     */
    public Identity authenticateUser(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AuthenticationFailedException("API Key 不能为空");
        }

        String prefix = extractKeyPrefix(apiKey);
        Optional<UserApiKey> userApiKeyOpt = userApiKeyGateway.findByKeyPrefix(prefix);

        if (userApiKeyOpt.isEmpty()) {
            log.warn("API Key 未找到: prefix={}", prefix);
            throw new AuthenticationFailedException("无效的 API Key");
        }

        UserApiKey userApiKey = userApiKeyOpt.get();

        // 通过 hash 验证完整密钥（不再解密做明文比较）
        String inputHash = encryptionService.hashKey(apiKey);
        if (!inputHash.equals(userApiKey.getKeyHash())) {
            log.warn("API Key 不匹配: prefix={}", prefix);
            throw new AuthenticationFailedException("无效的 API Key");
        }

        if (!userApiKey.isAvailable()) {
            log.warn("API Key 不可用: id={}, deleted={}", userApiKey.getId(), userApiKey.isDeleted());
            throw new AuthenticationFailedException("API Key 已禁用");
        }

        return Identity.of(
                userApiKey.getUserId(),
                "user",
                userApiKey.getId(),
                userApiKey.getApplicationId()
        );
    }

    /** 提取 Key 前缀（前 8 位） */
    private String extractKeyPrefix(String apiKey) {
        return apiKey.length() >= 8 ? apiKey.substring(0, 8) : apiKey;
    }
}