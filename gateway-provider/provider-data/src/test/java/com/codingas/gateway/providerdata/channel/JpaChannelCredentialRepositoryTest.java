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
import com.codingas.gateway.provider.encryption.CredentialEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaChannelCredentialRepository 单元测试：mock Repository + CredentialEncryptor 验证
 * 委托与凭证加解密逻辑
 *
 * <p>覆盖 JpaChannelCredentialRepository 全部 public 方法（save/findById/findByChannelId/
 * findActiveByChannelId/findDefaultByChannelId/deleteById）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaChannelCredentialRepository 单元测试")
class ChannelCredentialGatewayImplTest {

    @Mock
    private ChannelCredentialJpaRepository credentialRepository;

    @Mock
    private CredentialEncryptor encryptor;

    @InjectMocks
    private JpaChannelCredentialRepository gateway;

    private ChannelCredential sampleCredential(Long id, Long channelId) {
        ChannelCredential c = new ChannelCredential();
        c.setId(id);
        c.setChannelId(channelId);
        c.setName("主密钥");
        c.setApiKeyPlain("sk-1234567890");
        c.setApiKeyPrefix("sk-12345");
        c.setKeyAlias("alias-1");
        c.setWeight(100);
        c.setPriority(1);
        c.setLastUsedAt(Instant.parse("2026-08-01T00:00:00Z"));
        c.setCreatedBy(10L);
        c.setUpdatedBy(20L);
        return c;
    }

    private ChannelCredentialDo sampleDo(Long id, Long channelId) {
        ChannelCredentialDo doObj = new ChannelCredentialDo();
        doObj.setId(id);
        doObj.setChannelId(channelId);
        doObj.setName("主密钥");
        doObj.setApiKeyPrefix("sk-12345");
        doObj.setKeyAlias("alias-1");
        doObj.setWeight(100);
        doObj.setPriority(1);
        doObj.setLastUsedAt(Instant.parse("2026-08-01T00:00:00Z"));
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        return doObj;
    }

    @Test
    @DisplayName("save（创建）：加密明文 Key + 自动生成前缀 + toEntity 解密读回")
    void save_createEncryptsPlainKeyAndGeneratesPrefix() {
        ChannelCredential credential = sampleCredential(null, 1L);
        // 前缀为空，触发 save 内自动取明文前 8 位生成
        credential.setApiKeyPrefix(null);
        when(encryptor.encrypt("sk-1234567890")).thenReturn("encrypted-value");
        when(encryptor.decrypt("encrypted-value")).thenReturn("sk-1234567890");
        when(credentialRepository.save(any(ChannelCredentialDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelCredential result = gateway.save(credential);

        ArgumentCaptor<ChannelCredentialDo> captor = ArgumentCaptor.forClass(ChannelCredentialDo.class);
        verify(credentialRepository).save(captor.capture());
        ChannelCredentialDo written = captor.getValue();
        assertThat(written.getApiKeyEncrypted()).isEqualTo("encrypted-value");
        // 明文为空时自动取前 8 位作为前缀
        assertThat(written.getApiKeyPrefix()).isEqualTo("sk-12345");
        // 读方向：解密返回明文
        assertThat(result.getApiKeyPlain()).isEqualTo("sk-1234567890");
    }

    @Test
    @DisplayName("save（创建，无明文 Key）：跳过加密，保留已配置前缀")
    void save_createWithoutPlainKeySkipsEncryption() {
        ChannelCredential credential = sampleCredential(null, 1L);
        credential.setApiKeyPlain(null);
        when(credentialRepository.save(any(ChannelCredentialDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelCredential result = gateway.save(credential);

        ArgumentCaptor<ChannelCredentialDo> captor = ArgumentCaptor.forClass(ChannelCredentialDo.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getApiKeyEncrypted()).isNull();
        assertThat(captor.getValue().getApiKeyPrefix()).isEqualTo("sk-12345");
        // 无密文时不解密
        assertThat(result.getApiKeyPlain()).isNull();
        verify(encryptor, never()).encrypt(any());
    }

    @Test
    @DisplayName("save（更新，提供新明文）：重新加密")
    void save_updateWithNewPlainKeyReEncrypts() {
        ChannelCredential credential = sampleCredential(9L, 1L);
        credential.setApiKeyPlain("new-secret-key");
        when(encryptor.encrypt("new-secret-key")).thenReturn("new-encrypted");
        when(encryptor.decrypt("new-encrypted")).thenReturn("new-secret-key");
        when(credentialRepository.save(any(ChannelCredentialDo.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelCredential result = gateway.save(credential);

        ArgumentCaptor<ChannelCredentialDo> captor = ArgumentCaptor.forClass(ChannelCredentialDo.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getApiKeyEncrypted()).isEqualTo("new-encrypted");
        assertThat(result.getApiKeyPlain()).isEqualTo("new-secret-key");
        // 更新分支不查旧记录
        verify(credentialRepository, never()).findById(any());
    }

    @Test
    @DisplayName("save（更新，无新明文）：保留已有密文")
    void save_updateWithoutPlainKeyPreservesExistingEncrypted() {
        ChannelCredential credential = sampleCredential(9L, 1L);
        credential.setApiKeyPlain(null);

        ChannelCredentialDo existing = sampleDo(9L, 1L);
        existing.setApiKeyEncrypted("existing-encrypted");
        existing.setApiKeyPlain("existing-plain");
        when(credentialRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(credentialRepository.save(any(ChannelCredentialDo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encryptor.decrypt("existing-encrypted")).thenReturn("existing-plain");

        ChannelCredential result = gateway.save(credential);

        ArgumentCaptor<ChannelCredentialDo> captor = ArgumentCaptor.forClass(ChannelCredentialDo.class);
        verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getApiKeyEncrypted()).isEqualTo("existing-encrypted");
        assertThat(captor.getValue().getApiKeyPlain()).isEqualTo("existing-plain");
        assertThat(result.getApiKeyPlain()).isEqualTo("existing-plain");
    }

    @Test
    @DisplayName("findById：存在时解密返回明文，不存在返回空")
    void findById_decryptsWhenPresentOrEmpty() {
        ChannelCredentialDo doObj = sampleDo(1L, 1L);
        doObj.setApiKeyEncrypted("encrypted-value");
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(doObj));
        when(encryptor.decrypt("encrypted-value")).thenReturn("sk-1234567890");
        when(credentialRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<ChannelCredential> present = gateway.findById(1L);
        assertThat(present).isPresent();
        assertThat(present.get().getId()).isEqualTo(1L);
        assertThat(present.get().getChannelId()).isEqualTo(1L);
        assertThat(present.get().getName()).isEqualTo("主密钥");
        assertThat(present.get().getApiKeyPlain()).isEqualTo("sk-1234567890");
        assertThat(present.get().getKeyAlias()).isEqualTo("alias-1");

        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findById：解密失败时明文置空")
    void findById_returnsNullPlainKeyWhenDecryptFails() {
        ChannelCredentialDo doObj = sampleDo(1L, 1L);
        doObj.setApiKeyEncrypted("corrupt");
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(doObj));
        when(encryptor.decrypt("corrupt")).thenThrow(new RuntimeException("解密失败"));

        Optional<ChannelCredential> result = gateway.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getApiKeyPlain()).isNull();
    }

    @Test
    @DisplayName("findByChannelId / findActiveByChannelId / findByChannelIdAndState：按渠道查询并转换")
    void findByChannelId_delegatesAndConverts() {
        when(credentialRepository.findByChannelId(1L)).thenReturn(List.of(
                sampleDo(1L, 1L),
                sampleDo(2L, 1L)));

        assertThat(gateway.findByChannelId(1L)).hasSize(2);
        assertThat(gateway.findActiveByChannelId(1L)).hasSize(2);
        verify(credentialRepository, times(2)).findByChannelId(1L);
    }

    @Test
    @DisplayName("findDefaultByChannelId：返回活动凭证列表的第一个")
    void findDefaultByChannelId_returnsFirstActive() {
        when(credentialRepository.findByChannelId(1L)).thenReturn(List.of(
                sampleDo(1L, 1L),
                sampleDo(2L, 1L)));

        Optional<ChannelCredential> result = gateway.findDefaultByChannelId(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteById：委托 Repository 删除")
    void deleteById_delegates() {
        gateway.deleteById(3L);
        verify(credentialRepository).deleteById(3L);
    }

}
