package com.codingas.gateway.web.service;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.domain.enums.ProviderType;
import com.codingas.gateway.core.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderManageUseCase 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderManageUseCase Tests")
class ProviderManageUseCaseTest {

    @Mock
    private ProviderService providerService;

    @InjectMocks
    private ProviderManageUseCase providerManageUseCase;

    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setProviderCode("openai");
        testProvider.setProviderName("OpenAI");
        testProvider.setProviderType(ProviderType.OPENAI);
        testProvider.setWebsiteUrl("https://openai.com");
        testProvider.setApiDocUrl("https://platform.openai.com/docs");
        testProvider.setStatus(ProviderStatus.ACTIVE);
        testProvider.setPriority(100);
        testProvider.setBaseUrl("https://api.openai.com");
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("返回所有提供商")
        void findAll_returnsAllProviders() {
            when(providerService.findAll()).thenReturn(List.of(testProvider));

            List<Provider> result = providerManageUseCase.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderCode()).isEqualTo("openai");
            verify(providerService).findAll();
        }

        @Test
        @DisplayName("无提供商时返回空列表")
        void findAll_empty_returnsEmptyList() {
            when(providerService.findAll()).thenReturn(List.of());

            List<Provider> result = providerManageUseCase.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("存在则返回提供商")
        void findById_exists_returnsProvider() {
            when(providerService.findById(1L)).thenReturn(Optional.of(testProvider));

            Optional<Provider> result = providerManageUseCase.findById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getProviderCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("不存在则返回空")
        void findById_notExists_returnsEmpty() {
            when(providerService.findById(99L)).thenReturn(Optional.empty());

            Optional<Provider> result = providerManageUseCase.findById(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByProviderCode")
    class FindByProviderCodeTests {

        @Test
        @DisplayName("根据编码查找提供商")
        void findByProviderCode_exists_returnsProvider() {
            when(providerService.findByProviderCode("openai")).thenReturn(Optional.of(testProvider));

            Optional<Provider> result = providerManageUseCase.findByProviderCode("openai");

            assertThat(result).isPresent();
            assertThat(result.get().getProviderName()).isEqualTo("OpenAI");
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTests {

        @Test
        @DisplayName("根据状态返回提供商")
        void findByStatus_returnsProviders() {
            when(providerService.findByStatus(ProviderStatus.ACTIVE)).thenReturn(List.of(testProvider));

            List<Provider> result = providerManageUseCase.findByStatus(ProviderStatus.ACTIVE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("创建提供商并返回")
        void create_validProvider_returnsCreated() {
            Provider newProvider = new Provider();
            newProvider.setProviderCode("anthropic");
            newProvider.setProviderName("Anthropic");
            newProvider.setProviderType(ProviderType.ANTHROPIC);
            newProvider.setStatus(ProviderStatus.ACTIVE);
            when(providerService.create(any(Provider.class))).thenReturn(newProvider);

            Provider result = providerManageUseCase.create(newProvider);

            assertThat(result.getProviderCode()).isEqualTo("anthropic");
            verify(providerService).create(any(Provider.class));
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商并返回")
        void update_validProvider_returnsUpdated() {
            Provider updated = new Provider();
            updated.setProviderCode("openai");
            updated.setProviderName("OpenAI Updated");
            when(providerService.update(eq(1L), any(Provider.class))).thenReturn(updated);

            Provider result = providerManageUseCase.update(1L, updated);

            assertThat(result.getProviderName()).isEqualTo("OpenAI Updated");
            verify(providerService).update(eq(1L), any(Provider.class));
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商")
        void delete_validId_callsService() {
            doNothing().when(providerService).delete(1L);

            providerManageUseCase.delete(1L);

            verify(providerService).delete(1L);
        }
    }
}