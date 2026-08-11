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
package com.codingas.gateway.domain.iam.service;

import org.slf4j.Logger;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 生成器领域服务
 *
 * <p>生成格式：{@code sk-} + 32 字节随机数的 Base64（共 46 字符）。</p>
 * <p>生成时检测 keyPrefix 碰撞，最多重试 3 次。</p>
 */
@Service
public class DefaultUserApiKeyGenerator implements UserApiKeyGenerator {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserApiKeyGenerator.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_KEY_GENERATION_RETRIES = 5;
    public static final int KEY_PREFIX_LENGTH = 10;

    private final UserApiKeyGateway userApiKeyGateway;

    public DefaultUserApiKeyGenerator(UserApiKeyGateway userApiKeyGateway) {
        this.userApiKeyGateway = userApiKeyGateway;
    }

    /**
     * 生成唯一的 API Key，保证 prefix 不碰撞
     *
     * @return 生成结果（含明文 Key 和 prefix）
     * @throws IllegalStateException 超过重试次数仍碰撞
     */
    @Override
    public GeneratedApiKey generate() {
        for (int attempt = 0; attempt < MAX_KEY_GENERATION_RETRIES; attempt++) {
            String plainKey = generateRawKey();
            String keyPrefix = plainKey.substring(0, Math.min(KEY_PREFIX_LENGTH, plainKey.length()));

            if (userApiKeyGateway.findByKeyPrefix(keyPrefix).isPresent()) {
                log.warn("Key prefix 碰撞: {}, 重试 ({}/{})", keyPrefix, attempt + 1, MAX_KEY_GENERATION_RETRIES);
                continue;
            }

            return new GeneratedApiKey(plainKey, keyPrefix);
        }
        throw new IllegalStateException("无法生成唯一的 API Key，请重试");
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "sk-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
