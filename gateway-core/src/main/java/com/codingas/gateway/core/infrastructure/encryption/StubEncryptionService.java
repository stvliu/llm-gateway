package com.codingas.gateway.core.infrastructure.encryption;

/**
 * 加密服务stub实现
 *
 * <p>用于开发测试环境，生产环境应使用 AES-256 等真正加密。</p>
 */
public class StubEncryptionService implements EncryptionService {

    @Override
    public String encrypt(String plainText) {
        // TODO: 实现真正的 AES-256 加密
        return plainText;
    }

    @Override
    public String decrypt(String cipherText) {
        // TODO: 实现真正的 AES-256 解密
        return cipherText;
    }
}
