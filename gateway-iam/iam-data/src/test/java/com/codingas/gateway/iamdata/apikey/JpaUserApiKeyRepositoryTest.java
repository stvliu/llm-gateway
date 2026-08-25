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
package com.codingas.gateway.iamdata.apikey;

import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.encryption.ApiKeyEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaUserApiKeyRepository 单元测试
 *
 * <p>聚焦 applicationId 权限锚点字段在 DO↔Entity 转换中的正确传递，
 * 确保权限锚点由应用承载后，持久化层不丢失该字段。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUserApiKeyRepository 测试")
class JpaUserApiKeyRepositoryTest {

    @Mock
    private UserApiKeyJpaRepository repository;

    @Mock
    private ApiKeyEncryptor encryptionService;

    @InjectMocks
    private JpaUserApiKeyRepository gateway;

    @Nested
    @DisplayName("applicationId 映射测试")
    class ApplicationIdMappingTests {

        @Test
        @DisplayName("findById 将 DO 的 applicationId 映射到实体")
        void findById_mapsApplicationId() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setUserId(100L);
            doEntity.setApplicationId(7L);
            doEntity.setName("test-key");
            // keyEncrypted 留空，跳过解密路径，无需桩 encryptionService
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            Optional<UserApiKey> result = gateway.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getApplicationId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("save 将实体的 applicationId 映射到 DO")
        void save_mapsApplicationIdToDataObject() {
            UserApiKey entity = new UserApiKey();
            entity.setUserId(100L);
            entity.setApplicationId(7L);
            entity.setName("test-key");
            // keyPlain 留空，跳过 hash/encrypt 计算，避免依赖 encryptionService
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            savedDo.setUserId(100L);
            savedDo.setApplicationId(7L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getApplicationId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("查询方法测试")
    class QueryTests {

        @Test
        @DisplayName("findById 存在时返回实体并解密 Key")
        void findById_present_decryptsKey() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setUserId(100L);
            doEntity.setKeyEncrypted("encrypted-key");
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));
            when(encryptionService.decrypt("encrypted-key")).thenReturn("plain-key");

            Optional<UserApiKey> result = gateway.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getKeyPlain()).isEqualTo("plain-key");
        }

        @Test
        @DisplayName("findById 不存在时返回空")
        void findById_absent_returnsEmpty() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThat(gateway.findById(99L)).isEmpty();
        }

        @Test
        @DisplayName("解密失败时 Key 置空并返回实体")
        void findById_decryptFails_returnsNullKey() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setKeyEncrypted("encrypted-key");
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));
            when(encryptionService.decrypt("encrypted-key")).thenThrow(new RuntimeException("bad key"));

            Optional<UserApiKey> result = gateway.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getKeyPlain()).isNull();
        }

        @Test
        @DisplayName("findByUserId 返回列表并完成转换")
        void findByUserId_returnsConvertedList() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setUserId(100L);
            doEntity.setName("k1");
            when(repository.findByUserId(100L)).thenReturn(List.of(doEntity));

            List<UserApiKey> result = gateway.findByUserId(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(100L);
            assertThat(result.get(0).getName()).isEqualTo("k1");
        }

        @Test
        @DisplayName("findByApplicationId 返回列表并完成转换")
        void findByApplicationId_returnsConvertedList() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            doEntity.setApplicationId(7L);
            when(repository.findByApplicationId(7L)).thenReturn(List.of(doEntity));

            List<UserApiKey> result = gateway.findByApplicationId(7L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplicationId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("findAllNonDeleted 返回列表")
        void findAllNonDeleted_returnsList() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setId(1L);
            when(repository.findAllNonDeleted()).thenReturn(List.of(doEntity));

            List<UserApiKey> result = gateway.findAllNonDeleted();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("findByKeyPrefix 存在时返回实体")
        void findByKeyPrefix_present_returnsEntity() {
            UserApiKeyDo doEntity = new UserApiKeyDo();
            doEntity.setKeyPrefix("sk-test-12");
            when(repository.findByKeyPrefix("sk-test-12")).thenReturn(Optional.of(doEntity));

            Optional<UserApiKey> result = gateway.findByKeyPrefix("sk-test-12");

            assertThat(result).isPresent();
            assertThat(result.get().getKeyPrefix()).isEqualTo("sk-test-12");
        }
    }

    @Nested
    @DisplayName("save 加解密分支测试")
    class SaveEncryptionTests {

        @Test
        @DisplayName("创建时计算哈希与密文并生成前缀")
        void save_create_computesHashEncryptAndPrefix() {
            UserApiKey entity = new UserApiKey();
            entity.setUserId(100L);
            entity.setName("k1");
            entity.setKeyPlain("sk-very-long-key-1234567890");
            when(encryptionService.hashKey("sk-very-long-key-1234567890")).thenReturn("hash");
            when(encryptionService.encrypt("sk-very-long-key-1234567890")).thenReturn("enc");
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            UserApiKeyDo written = captor.getValue();
            assertThat(written.getKeyHash()).isEqualTo("hash");
            assertThat(written.getKeyEncrypted()).isEqualTo("enc");
            // 前缀取明文前 10 位
            assertThat(written.getKeyPrefix()).isEqualTo("sk-very-lo");
            assertThat(written.isDeleted()).isFalse();
            assertThat(written.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("创建时明文为空白则不计算加密")
        void save_create_blankPlain_skipsEncryption() {
            UserApiKey entity = new UserApiKey();
            entity.setUserId(100L);
            entity.setKeyPlain("  ");
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getKeyHash()).isNull();
            assertThat(captor.getValue().getKeyEncrypted()).isNull();
            verify(encryptionService, never()).hashKey(any());
            verify(encryptionService, never()).encrypt(any());
        }

        @Test
        @DisplayName("更新时保留已有哈希与密文")
        void save_update_keepsExistingHashAndEncrypted() {
            UserApiKey entity = new UserApiKey();
            entity.setId(1L);
            entity.setKeyPlain("new-plain");
            UserApiKeyDo existing = new UserApiKeyDo();
            existing.setKeyHash("old-hash");
            existing.setKeyEncrypted("old-enc");
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getKeyHash()).isEqualTo("old-hash");
            assertThat(captor.getValue().getKeyEncrypted()).isEqualTo("old-enc");
            // 更新路径不重新加密
            verify(encryptionService, never()).encrypt(any());
        }
    }

    @Nested
    @DisplayName("软删除测试")
    class DeleteTests {

        @Test
        @DisplayName("delete 存在时软删除并更新时间")
        void delete_present_softDeletes() {
            UserApiKey entity = new UserApiKey();
            entity.setId(1L);
            UserApiKeyDo existing = new UserApiKeyDo();
            existing.setId(1L);
            existing.setDeleted(false);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            UserApiKeyDo savedDo = new UserApiKeyDo();
            savedDo.setId(1L);
            when(repository.save(any())).thenReturn(savedDo);

            gateway.delete(entity);

            ArgumentCaptor<UserApiKeyDo> captor = ArgumentCaptor.forClass(UserApiKeyDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("delete 不存在时不保存")
        void delete_absent_doesNotSave() {
            UserApiKey entity = new UserApiKey();
            entity.setId(99L);
            when(repository.findById(99L)).thenReturn(Optional.empty());

            gateway.delete(entity);

            verify(repository, never()).save(any());
        }
    }
}
