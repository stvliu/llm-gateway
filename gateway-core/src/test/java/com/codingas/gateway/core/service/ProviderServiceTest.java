package com.codingas.gateway.core.service;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.domain.enums.ProviderType;
import com.codingas.gateway.core.domain.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderService 单元测试
 *
 * <p>测试 ProviderService 通过 ProviderGateway 接口访问持久化数据。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderService Tests")
class ProviderServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProviderService providerService;

    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setProviderCode("openai");
        testProvider.setProviderName("OpenAI");
        testProvider.setProviderType(ProviderType.OPENAI);
        testProvider.setBaseUrl("https://api.openai.com");
        testProvider.setPriority(100);
        testProvider.setStatus(ProviderStatus.ACTIVE);
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("返回 Provider 当存在时")
        void findById_existingProvider() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));

            Optional<Provider> result = providerService.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("返回空 Optional 当不存在时")
        void findById_notFound() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            Optional<Provider> result = providerService.findById(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProviderCode")
    class FindByProviderCodeTests {

        @Test
        @DisplayName("根据 Provider 编码查询成功")
        void findByProviderCode_success() {
            when(providerGateway.findByProviderCode("openai")).thenReturn(Optional.of(testProvider));

            Optional<Provider> result = providerService.findByProviderCode("openai");

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("根据 Provider 编码查询失败")
        void findByProviderCode_notFound() {
            when(providerGateway.findByProviderCode("unknown")).thenReturn(Optional.empty());

            Optional<Provider> result = providerService.findByProviderCode("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有活跃 Provider")
        void findAll_returnsActiveProviders() {
            List<Provider> providers = List.of(testProvider);
            when(providerGateway.findAllActive()).thenReturn(providers);

            List<Provider> result = providerService.findAll();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("返回空列表当没有活跃 Provider 时")
        void findAll_emptyList() {
            when(providerGateway.findAllActive()).thenReturn(List.of());

            List<Provider> result = providerService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTests {

        @Test
        @DisplayName("根据状态查询 Provider")
        void findByStatus_returnsProviders() {
            List<Provider> providers = List.of(testProvider);
            when(providerGateway.findByStatus(ProviderStatus.ACTIVE)).thenReturn(providers);

            List<Provider> result = providerService.findByStatus(ProviderStatus.ACTIVE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建 Provider 成功")
        void create_success() {
            Provider newProvider = new Provider();
            newProvider.setProviderCode("anthropic");
            newProvider.setProviderName("Anthropic");
            newProvider.setProviderType(ProviderType.ANTHROPIC);
            newProvider.setBaseUrl("https://api.anthropic.com");
            newProvider.setPriority(90);

            when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(2L);
                return p;
            });

            Provider result = providerService.create(newProvider);

            assertThat(result.getId()).isEqualTo(2L);

            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerGateway).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        }

        @Test
        @DisplayName("创建时如果未设置状态则默认为 ACTIVE")
        void create_defaultStatus() {
            Provider newProvider = new Provider();
            newProvider.setProviderCode("test");
            newProvider.setProviderName("Test");
            newProvider.setProviderType(ProviderType.OPENAI);
            // status 未设置

            when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> {
                Provider p = inv.getArgument(0);
                p.setId(3L);
                return p;
            });

            providerService.create(newProvider);

            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerGateway).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        }

        @Test
        @DisplayName("创建后发布配置变更事件")
        void create_publishesEvent() {
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            providerService.create(testProvider);

            verify(eventPublisher).publishEvent(any(ProviderService.ProviderConfigChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新 Provider 成功")
        void update_success() {
            Provider updatedProvider = new Provider();
            updatedProvider.setProviderName("OpenAI Updated");
            updatedProvider.setProviderType(ProviderType.OPENAI);
            updatedProvider.setBaseUrl("https://api.openai.com/v1");
            updatedProvider.setPriority(150);
            updatedProvider.setStatus(ProviderStatus.ACTIVE);

            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenAnswer(inv -> inv.getArgument(0));

            Provider result = providerService.update(1L, updatedProvider);

            assertThat(result.getProviderName()).isEqualTo("OpenAI Updated");
            assertThat(result.getBaseUrl()).isEqualTo("https://api.openai.com/v1");
            assertThat(result.getPriority()).isEqualTo(150);
        }

        @Test
        @DisplayName("更新不存在的 Provider 抛出异常")
        void update_notFound() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            Provider updatedProvider = new Provider();
            updatedProvider.setProviderName("Updated");

            assertThatThrownBy(() -> providerService.update(99L, updatedProvider))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Provider not found: 99");
        }

        @Test
        @DisplayName("更新后发布配置变更事件")
        void update_publishesEvent() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            Provider updatedProvider = new Provider();
            updatedProvider.setProviderName("Updated");
            providerService.update(1L, updatedProvider);

            verify(eventPublisher).publishEvent(any(ProviderService.ProviderConfigChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除 Provider 成功 (软删除)")
        void delete_success() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            providerService.delete(1L);

            ArgumentCaptor<Provider> captor = ArgumentCaptor.forClass(Provider.class);
            verify(providerGateway).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ProviderStatus.DELETED);
        }

        @Test
        @DisplayName("删除不存在的 Provider 抛出异常")
        void delete_notFound() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.delete(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Provider not found: 99");
        }

        @Test
        @DisplayName("删除后发布配置变更事件")
        void delete_publishesEvent() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            providerService.delete(1L);

            verify(eventPublisher).publishEvent(any(ProviderService.ProviderConfigChangedEvent.class));
        }
    }
}
