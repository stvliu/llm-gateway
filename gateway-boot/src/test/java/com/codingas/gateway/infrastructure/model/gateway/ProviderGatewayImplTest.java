package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.infrastructure.model.gateway.database.ProviderRepository;
import com.codingas.gateway.infrastructure.model.gateway.database.dataobject.ProviderDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderGatewayImpl 测试")
class ProviderGatewayImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 Provider 成功")
        void save_validEntity_returnsSaved() {
            // given
            Provider entity = createTestEntity();
            ProviderDo savedDo = createTestDo();

            when(providerRepository.save(any())).thenReturn(savedDo);

            // when
            Provider result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getProviderCode()).isEqualTo("openai");
            verify(providerRepository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 Provider 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            ProviderDo doEntity = createTestDo();
            when(providerRepository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<Provider> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(providerRepository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Provider> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProviderCode 方法测试")
    class FindByProviderCodeTests {

        @Test
        @DisplayName("通过编码找到 Provider")
        void findByProviderCode_existingCode_returnsEntity() {
            // given
            ProviderDo doEntity = createTestDo();
            when(providerRepository.findByProviderCode("openai")).thenReturn(Optional.of(doEntity));

            // when
            Optional<Provider> result = gateway.findByProviderCode("openai");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("未找到返回空")
        void findByProviderCode_nonExistingCode_returnsEmpty() {
            // given
            when(providerRepository.findByProviderCode("unknown")).thenReturn(Optional.empty());

            // when
            Optional<Provider> result = gateway.findByProviderCode("unknown");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有 Provider")
        void findAll_returnsAll() {
            // given
            ProviderDo doEntity1 = createTestDo();
            ProviderDo doEntity2 = createTestDo();
            doEntity2.setId(2L);
            doEntity2.setProviderCode("anthropic");
            when(providerRepository.findAll()).thenReturn(List.of(doEntity1, doEntity2));

            // when
            List<Provider> result = gateway.findAll();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAllActive 方法测试")
    class FindAllActiveTests {

        @Test
        @DisplayName("返回所有活跃 Provider")
        void findAllActive_returnsActiveProviders() {
            // given
            ProviderDo doEntity = createTestDo();
            when(providerRepository.findByStatus(ProviderDo.ProviderStatus.ACTIVE)).thenReturn(List.of(doEntity));

            // when
            List<Provider> result = gateway.findAllActive();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(Provider.ProviderStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("findByStatus 方法测试")
    class FindByStatusTests {

        @Test
        @DisplayName("通过状态找到 Provider 列表")
        void findByStatus_existingStatus_returnsList() {
            // given
            ProviderDo doEntity = createTestDo();
            when(providerRepository.findByStatus(ProviderDo.ProviderStatus.ACTIVE)).thenReturn(List.of(doEntity));

            // when
            List<Provider> result = gateway.findByStatus(Provider.ProviderStatus.ACTIVE);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("count 方法测试")
    class CountTests {

        @Test
        @DisplayName("返回总数")
        void count_returnsCount() {
            // given
            when(providerRepository.count()).thenReturn(5L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除成功")
        void delete_existingEntity_deletes() {
            // given
            Provider entity = createTestEntity();
            doNothing().when(providerRepository).delete(any());

            // when
            gateway.delete(entity);

            // then
            verify(providerRepository).delete(any());
        }
    }

    @Nested
    @DisplayName("existsByProviderCode 方法测试")
    class ExistsByProviderCodeTests {

        @Test
        @DisplayName("编码存在返回 true")
        void existsByProviderCode_existingCode_returnsTrue() {
            // given
            when(providerRepository.existsByProviderCode("openai")).thenReturn(true);

            // when
            boolean result = gateway.existsByProviderCode("openai");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("编码不存在返回 false")
        void existsByProviderCode_nonExistingCode_returnsFalse() {
            // given
            when(providerRepository.existsByProviderCode("unknown")).thenReturn(false);

            // when
            boolean result = gateway.existsByProviderCode("unknown");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getMaxVersion 方法测试")
    class GetMaxVersionTests {

        @Test
        @DisplayName("返回最大版本号")
        void getMaxVersion_returnsMaxVersion() {
            // given
            when(providerRepository.findMaxVersion()).thenReturn(3L);

            // when
            long result = gateway.getMaxVersion();

            // then
            assertThat(result).isEqualTo(3L);
        }

        @Test
        @DisplayName("无版本时返回 0")
        void getMaxVersion_noVersions_returnsZero() {
            // given
            when(providerRepository.findMaxVersion()).thenReturn(null);

            // when
            long result = gateway.getMaxVersion();

            // then
            assertThat(result).isEqualTo(0L);
        }
    }

    // Helper methods
    private Provider createTestEntity() {
        Provider entity = new Provider();
        entity.setId(1L);
        entity.setProviderCode("openai");
        entity.setProviderName("OpenAI");
        entity.setProviderType(ProviderType.OPENAI);
        entity.setBaseUrl("https://api.openai.com");
        entity.setPriority(100);
        entity.setStatus(Provider.ProviderStatus.ACTIVE);
        return entity;
    }

    private ProviderDo createTestDo() {
        ProviderDo doEntity = new ProviderDo();
        doEntity.setId(1L);
        doEntity.setProviderCode("openai");
        doEntity.setProviderName("OpenAI");
        doEntity.setProviderType(ProviderType.OPENAI);
        doEntity.setBaseUrl("https://api.openai.com");
        doEntity.setPriority(100);
        doEntity.setStatus(ProviderDo.ProviderStatus.ACTIVE);
        doEntity.setCreatedAt(Instant.now());
        doEntity.setUpdatedAt(Instant.now());
        return doEntity;
    }
}
