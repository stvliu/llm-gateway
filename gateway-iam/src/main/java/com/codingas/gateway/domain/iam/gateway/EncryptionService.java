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
package com.codingas.gateway.domain.iam.gateway;

/**
 * 加密服务接口
 *
 * <p>提供 API Key 等敏感数据的加密/解密功能。</p>
 */
public interface EncryptionService {

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
     * @param cipherText 密文
     * @return 明文
     */
    String decrypt(String cipherText);
}
