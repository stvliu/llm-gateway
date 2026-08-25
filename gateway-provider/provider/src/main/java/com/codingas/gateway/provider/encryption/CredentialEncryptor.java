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
package com.codingas.gateway.provider.encryption;

/**
 * 渠道凭证加解密契约（依赖倒置）。
 *
 * <p>供给域基础设施在落库/回读渠道 API Key 时，通过本接口完成加解密，
 * 不反向依赖 IAM 域的具体加密实现。实现由 IAM 域（或组装层）提供并注入。</p>
 */
public interface CredentialEncryptor {

    /**
     * 加密明文
     *
     * @param plainText 明文
     * @return 密文
     */
    String encrypt(String plainText);

    /**
     * 解密密文
     *
     * @param encryptedText 密文
     * @return 明文
     */
    String decrypt(String encryptedText);
}
