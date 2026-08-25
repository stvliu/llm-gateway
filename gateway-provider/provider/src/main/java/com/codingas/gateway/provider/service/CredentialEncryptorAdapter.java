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
package com.codingas.gateway.provider.service;

import com.codingas.gateway.iam.service.ApiKeyEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CredentialEncryptor 适配实现（组装层桥接）。
 *
 * <p>供给域基础设施通过 {@link CredentialEncryptor} 契约解耦对 IAM 加密服务的依赖；
 * 本类在组装层把该契约桥接到 IAM 域的 {@link ApiKeyEncryptionService}。
 * P3 下沉 provider 门面后，gateway-provider 需依赖 gateway-iam（ModelDiscoveryService 查询
 * 应用渠道），为避免模块依赖环（provider→iam→provider），本桥接类由 gateway-iam 迁至
 * boot 组装层（boot 同时依赖 provider 与 iam）。</p>
 */
@Component
@RequiredArgsConstructor
public class CredentialEncryptorAdapter implements CredentialEncryptor {

    private final ApiKeyEncryptionService encryptionService;

    @Override
    public String encrypt(String plainText) {
        return encryptionService.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedText) {
        return encryptionService.decrypt(encryptedText);
    }
}
