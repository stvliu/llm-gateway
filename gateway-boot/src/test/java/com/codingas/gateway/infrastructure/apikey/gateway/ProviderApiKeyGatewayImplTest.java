package com.codingas.gateway.infrastructure.apikey.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderApiKeyRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderApiKeyDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderApiKeyGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderApiKeyGatewayImpl 测试")
class ProviderApiKeyGatewayImplTest {

    @Mock
    private ProviderApiKeyRepository repository;

    @InjectMocks
    private ProviderApiKeyGatewayImpl gateway;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("通过 ID 找到 API Key")
        void findById_existingId_returnsEntity() {
            // given
            ProviderApiKeyDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<ProviderApiKey> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("findByProviderId 方法测试")
    class FindByProviderIdTests {

        @Test
        @DisplayName("通过提供商 ID 找到 API Key")
        void findByProviderId_existingProviderId_returnsEntity() {
            // given
            ProviderApiKeyDo doEntity = createTestDo();
            when(repository.findByProviderId(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<ProviderApiKey> result = gateway.findByProviderId(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProviderId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 API Key 成功")
        void save_validEntity_returnsSaved() {
            // given
            ProviderApiKey entity = createTestEntity();
            ProviderApiKeyDo savedDo = createTestDo();

            when(repository.save(any())).thenReturn(savedDo);

            // when
            ProviderApiKey result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("getMaxVersion 方法测试")
    class GetMaxVersionTests {

        @Test
        @DisplayName("返回最大版本号")
        void getMaxVersion_returnsMaxVersion() {
            // given
            when(repository.findMaxVersion()).thenReturn(5L);

            // when
            long result = gateway.getMaxVersion();

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("无版本时返回 0")
        void getMaxVersion_noVersions_returnsZero() {
            // given
            when(repository.findMaxVersion()).thenReturn(null);

            // when
            long result = gateway.getMaxVersion();

            // then
            assertThat(result).isEqualTo(0L);
        }
    }

    // Helper methods
    private ProviderApiKey createTestEntity() {
        ProviderApiKey entity = new ProviderApiKey();
        entity.setId(1L);
        entity.setProviderId(1L);
        entity.setKeyName("OpenAI API Key");
        entity.setApiKey("sk-test-key");
        entity.setPriority(100);
        entity.setStatus(ProviderApiKey.ProviderApiKeyStatus.ACTIVE);
        return entity;
    }

    private ProviderApiKeyDo createTestDo() {
        ProviderApiKeyDo doEntity = new ProviderApiKeyDo();
        doEntity.setId(1L);
        doEntity.setProviderId(1L);
        doEntity.setKeyName("OpenAI API Key");
        doEntity.setApiKey("sk-test-key");
        doEntity.setPriority(100);
        doEntity.setStatus(ProviderApiKeyDo.ProviderApiKeyStatus.ACTIVE);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());
        return doEntity;
    }
}
