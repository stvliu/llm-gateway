package com.codingas.gateway.domain.model.service;

import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.codingas.gateway.domain.model.service.ProviderDomainService.ProviderConfigChangedEvent;

/**
 * ProviderDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderDomainService 测试")
class ProviderDomainServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProviderDomainService service;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到 Provider 返回实体")
        void findById_existingId_returnsEntity() {
            // given
            Provider provider = createTestProvider();
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            Optional<Provider> result = service.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(providerGateway.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<Provider> result = service.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回所有活跃 Provider")
        void findAll_returnsActiveProviders() {
            // given
            Provider provider = createTestProvider();
            when(providerGateway.findAllActive()).thenReturn(List.of(provider));

            // when
            List<Provider> result = service.findAll();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建 Provider 成功")
        void create_validProvider_returnsCreated() {
            // given
            Provider provider = createTestProvider();
            provider.setState(ProviderState.ACTIVE); // 测试默认状态设置
            when(providerGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Provider result = service.create(provider);

            // then
            assertThat(result.getState()).isEqualTo(ProviderState.ACTIVE);
            verify(eventPublisher).publishEvent(any(ProviderConfigChangedEvent.class));
        }

        @Test
        @DisplayName("创建时保留已有状态")
        void create_withStatus_preservesStatus() {
            // given
            Provider provider = createTestProvider();
            provider.setState(ProviderState.ACTIVE);
            when(providerGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Provider result = service.create(provider);

            // then
            assertThat(result.getState()).isEqualTo(ProviderState.ACTIVE);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新 Provider 成功")
        void update_existingProvider_returnsUpdated() {
            // given
            Provider existing = createTestProvider();
            Provider updateData = new Provider();
            updateData.setName("Updated Name");
            updateData.setType(ProviderType.ANTHROPIC);
            updateData.setBaseUrl("https://new.url");
            updateData.setPriority(50);
            updateData.setState(ProviderState.ACTIVE);

            when(providerGateway.findById(1L)).thenReturn(Optional.of(existing));
            when(providerGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            Provider result = service.update(1L, updateData);

            // then
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getPriority()).isEqualTo(50);
            verify(eventPublisher).publishEvent(any(ProviderConfigChangedEvent.class));
        }

        @Test
        @DisplayName("更新不存在的 Provider 抛出异常")
        void update_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.update(999L, new Provider()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除 Provider 成功")
        void delete_existingProvider_deletesSuccessfully() {
            // given
            Provider provider = createTestProvider();
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            doNothing().when(providerGateway).delete(provider);

            // when
            service.delete(1L);

            // then
            verify(providerGateway).delete(provider);
            verify(eventPublisher).publishEvent(any(ProviderConfigChangedEvent.class));
        }

        @Test
        @DisplayName("删除不存在的 Provider 抛出异常")
        void delete_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.delete(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider not found");
        }
    }

    // Helper methods
    private Provider createTestProvider() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setName("OpenAI");
        provider.setType(ProviderType.OPENAI);
        provider.setBaseUrl("https://api.openai.com");
        provider.setPriority(100);
        provider.setState(ProviderState.ACTIVE);
        return provider;
    }
}
