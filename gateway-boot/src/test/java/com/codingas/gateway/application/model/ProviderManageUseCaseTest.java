package com.codingas.gateway.application.model;

import com.codingas.gateway.application.provider.ProviderManageUseCase;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.service.ProviderDomainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ProviderManageUseCase 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderManageUseCase 单元测试")
class ProviderManageUseCaseTest {

    @Mock
    private ProviderDomainService providerService;

    @InjectMocks
    private ProviderManageUseCase providerManageUseCase;

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有提供商")
        void findAll_returnsAllProviders() {
            // given
            Provider provider1 = createProvider(1L, "OpenAI");
            Provider provider2 = createProvider(2L, "Anthropic");
            when(providerService.findAll()).thenReturn(List.of(provider1, provider2));

            // when
            List<Provider> result = providerManageUseCase.findAll();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("OpenAI");
            assertThat(result.get(1).getName()).isEqualTo("Anthropic");
            verify(providerService).findAll();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("提供商存在时返回提供商")
        void findById_existingProvider_returnsProvider() {
            // given
            Long id = 1L;
            Provider provider = createProvider(id, "OpenAI");
            when(providerService.findById(id)).thenReturn(Optional.of(provider));

            // when
            Optional<Provider> result = providerManageUseCase.findById(id);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("OpenAI");
            verify(providerService).findById(id);
        }

        @Test
        @DisplayName("提供商不存在时返回空 Optional")
        void findById_nonExistingProvider_returnsEmpty() {
            // given
            Long id = 99L;
            when(providerService.findById(id)).thenReturn(Optional.empty());

            // when
            Optional<Provider> result = providerManageUseCase.findById(id);

            // then
            assertThat(result).isEmpty();
            verify(providerService).findById(id);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建提供商成功")
        void create_validProvider_returnsCreatedProvider() {
            // given
            Provider inputProvider = createProvider(null, "OpenAI");
            Provider savedProvider = createProvider(1L, "OpenAI");
            when(providerService.create(any(Provider.class))).thenReturn(savedProvider);

            // when
            Provider result = providerManageUseCase.create(inputProvider);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("OpenAI");
            verify(providerService).create(any(Provider.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商成功")
        void update_validProvider_returnsUpdatedProvider() {
            // given
            Long id = 1L;
            Provider updateProvider = createProvider(null, "OpenAI Updated");
            Provider updatedProvider = createProvider(id, "OpenAI Updated");
            when(providerService.update(eq(id), any(Provider.class))).thenReturn(updatedProvider);

            // when
            Provider result = providerManageUseCase.update(id, updateProvider);

            // then
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getName()).isEqualTo("OpenAI Updated");
            verify(providerService).update(eq(id), any(Provider.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商成功")
        void delete_existingProvider_deletesSuccessfully() {
            // given
            Long id = 1L;
            doNothing().when(providerService).delete(id);

            // when
            providerManageUseCase.delete(id);

            // then
            verify(providerService).delete(id);
        }
    }

    /**
     * 创建测试用 Provider 对象
     */
    private Provider createProvider(Long id, String providerName) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(providerName);
        provider.setPriority(1);
        provider.setState(ProviderState.ACTIVE);
        return provider;
    }
}
