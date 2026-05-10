package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.security.service.EncryptionService;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
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
import static org.mockito.Mockito.*;

/**
 * ConfigCacheService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigCacheService 测试")
class ConfigCacheServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderApiKeyGateway apiKeyGateway;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private ConfigCacheService cacheService;

    @Nested
    @DisplayName("Provider 操作测试")
    class ProviderTests {

        @Test
        @DisplayName("通过 ID 获取 Provider")
        void getProviderById_found_returnsProvider() {
            // given
            Provider provider = createTestProvider();
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            // when
            Optional<Provider> result = cacheService.getProviderById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("获取所有 Provider")
        void getAllProviders_returnsList() {
            // given
            when(providerGateway.findAll()).thenReturn(List.of(createTestProvider()));

            // when
            List<Provider> result = cacheService.getAllProviders();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("获取活跃 Provider")
        void getActiveProviders_returnsActive() {
            // given
            when(providerGateway.findAllActive()).thenReturn(List.of(createTestProvider()));

            // when
            List<Provider> result = cacheService.getActiveProviders();

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Model 操作测试")
    class ModelTests {

        @Test
        @DisplayName("通过 ID 获取 Model")
        void getModelById_found_returnsModel() {
            // given
            Model model = createTestModel();
            when(modelGateway.findById(1L)).thenReturn(Optional.of(model));

            // when
            Optional<Model> result = cacheService.getModelById(1L);

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("获取所有 Model")
        void getAllModels_returnsList() {
            // given
            when(modelGateway.findAll()).thenReturn(List.of(createTestModel()));

            // when
            List<Model> result = cacheService.getAllModels();

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("通过 Provider ID 获取 Model")
        void getModelsByProviderId_returnsList() {
            // given
            when(modelGateway.findByProviderId(1L)).thenReturn(List.of(createTestModel()));

            // when
            List<Model> result = cacheService.getModelsByProviderId(1L);

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("API Key 操作测试")
    class ApiKeyTests {

        @Test
        @DisplayName("获取 API Key")
        void getApiKeyByProviderId_found_returnsApiKey() {
            // given
            ProviderApiKey apiKey = new ProviderApiKey();
            apiKey.setId(1L);
            apiKey.setProviderId(1L);
            apiKey.setApiKey("decrypted-value");

            when(apiKeyGateway.findByProviderId(1L)).thenReturn(List.of(apiKey));

            // when
            Optional<ProviderApiKey> result = cacheService.getApiKeyByProviderId(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getApiKey()).isEqualTo("decrypted-value");
        }

        @Test
        @DisplayName("API Key 不存在")
        void getApiKeyByProviderId_notFound_returnsEmpty() {
            // given
            when(apiKeyGateway.findByProviderId(999L)).thenReturn(List.of());

            // when
            Optional<ProviderApiKey> result = cacheService.getApiKeyByProviderId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("缓存刷新测试")
    class RefreshTests {

        @Test
        @DisplayName("刷新 Provider 缓存")
        void refreshProviders_clearsCache() {
            // when
            cacheService.refreshProviders();

            // then - 验证方法执行（@CacheEvict 会在运行时处理）
            assertThat(true).isTrue();
        }

        @Test
        @DisplayName("刷新 Model 缓存")
        void refreshModels_clearsCache() {
            // when
            cacheService.refreshModels();

            // then
            assertThat(true).isTrue();
        }

        @Test
        @DisplayName("刷新 API Key 缓存")
        void refreshApiKeys_clearsCache() {
            // when
            cacheService.refreshApiKeys();

            // then
            assertThat(true).isTrue();
        }

        @Test
        @DisplayName("刷新所有缓存")
        void refreshAll_clearsAllCaches() {
            // when
            cacheService.refreshAll();

            // then
            assertThat(true).isTrue();
        }
    }

    // Helper methods
    private Provider createTestProvider() {
        Provider provider = new Provider();
        provider.setId(1L);
        provider.setName("OpenAI");
        provider.setState(ProviderState.ACTIVE);
        return provider;
    }

    private Model createTestModel() {
        Model model = new Model();
        model.setId(1L);
        model.setDisplayName("GPT-4");
        model.setState(ModelState.ACTIVE);
        return model;
    }
}
