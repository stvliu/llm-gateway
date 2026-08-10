/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.iam.gateway.encryption;

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
