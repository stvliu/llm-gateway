package com.codingas.gateway.application.provider;

import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.gateway.ConnectivityTester;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderService 单元测试")
class ProviderServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ProviderApiKeyGateway providerApiKeyGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ConnectivityTester connectivityTester;

    @InjectMocks
    private ProviderServiceImpl providerService;

    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = createTestProvider(1L, "OpenAI", true);
    }

    @Nested
    @DisplayName("create 创建提供商")
    class CreateTests {

        @Test
        @DisplayName("创建提供商成功")
        void create_validRequest_returnsProviderResponse() {
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderName("Anthropic");
            request.setPriority(50);

            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> {
                Provider provider = invocation.getArgument(0);
                provider.setId(2L);
                return provider;
            });

            ProviderResponse response = providerService.create(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getProviderName()).isEqualTo("Anthropic");
            assertThat(response.getPriority()).isEqualTo(50);
            assertThat(response.getState()).isEqualTo("ACTIVE");
            verify(providerGateway).save(any(Provider.class));
        }

        @Test
        @DisplayName("创建时未指定优先级则使用默认值100")
        void create_noPriorityProvided_usesDefaultPriority() {
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderName("New Provider");

            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> {
                Provider provider = invocation.getArgument(0);
                provider.setId(3L);
                return provider;
            });

            ProviderResponse response = providerService.create(request);

            assertThat(response.getPriority()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getById 获取提供商")
    class GetByIdTests {

        @Test
        @DisplayName("提供商存在时返回提供商响应")
        void getById_existingProvider_returnsProviderResponse() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));

            ProviderResponse response = providerService.getById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
            verify(providerGateway).findById(1L);
        }

        @Test
        @DisplayName("提供商不存在时抛出 ResourceNotFoundException")
        void getById_nonExistingProvider_throwsException() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("query 查询提供商列表")
    class QueryTests {

        @Test
        @DisplayName("无过滤条件时返回所有提供商")
        void query_noFilter_returnsAllProviders() {
            Provider provider2 = createTestProvider(2L, "Anthropic", true);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, provider2));
            when(providerApiKeyGateway.getKeyStatsByProviderIds(any())).thenReturn(java.util.Map.of());

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setPage(1);
            request.setLimit(20);

            PageResponse<ProviderResponse> response = providerService.query(request);

            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("按关键字过滤提供商")
        void query_withKeyword_filtersProviders() {
            when(providerGateway.findAll()).thenReturn(List.of(testProvider));
            when(providerApiKeyGateway.getKeyStatsByProviderIds(any())).thenReturn(java.util.Map.of());

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("open");
            request.setPage(1);
            request.setLimit(20);

            PageResponse<ProviderResponse> response = providerService.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("按状态过滤提供商")
        void query_withStatus_filtersProviders() {
            Provider suspendedProvider = createTestProvider(2L, "Suspended Provider", false);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, suspendedProvider));
            when(providerApiKeyGateway.getKeyStatsByProviderIds(any())).thenReturn(java.util.Map.of());

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setState("ACTIVE");
            request.setPage(1);
            request.setLimit(20);

            PageResponse<ProviderResponse> response = providerService.query(request);

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().getFirst().getState()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("分页查询返回正确的数据")
        void query_withPagination_returnsPagedProviders() {
            List<Provider> providers = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                providers.add(createTestProvider(i, "Provider " + i, true));
            }
            when(providerGateway.findAll()).thenReturn(providers);
            when(providerApiKeyGateway.getKeyStatsByProviderIds(any())).thenReturn(java.util.Map.of());

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setPage(2);
            request.setLimit(10);

            PageResponse<ProviderResponse> response = providerService.query(request);

            assertThat(response.getItems()).hasSize(10);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getLimit()).isEqualTo(10);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void query_noMatch_returnsEmptyList() {
            when(providerGateway.findAll()).thenReturn(List.of(testProvider));
            when(providerApiKeyGateway.getKeyStatsByProviderIds(any())).thenReturn(java.util.Map.of());

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("nonexistent");
            request.setPage(1);
            request.setLimit(20);

            PageResponse<ProviderResponse> response = providerService.query(request);

            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("update 更新提供商")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商名称成功")
        void update_validName_updatesProvider() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("OpenAI Updated");

            ProviderResponse response = providerService.update(1L, request);

            assertThat(response).isNotNull();
            verify(providerGateway).findById(1L);
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("更新不存在的提供商抛出异常")
        void update_nonExistingProvider_throwsException() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("Updated Name");

            assertThatThrownBy(() -> providerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete 删除提供商")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商成功")
        void delete_existingProvider_deletes() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerApiKeyGateway.findByProviderId(1L)).thenReturn(List.of());
            when(modelGateway.findByProviderId(1L)).thenReturn(List.of());

            providerService.delete(1L);

            verify(providerGateway).delete(testProvider);
        }

        @Test
        @DisplayName("删除不存在的提供商抛出异常")
        void delete_nonExistingProvider_throwsException() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setEnabled 启用/禁用提供商")
    class SetEnabledTests {

        @Test
        @DisplayName("启用提供商成功")
        void setEnabled_true_enablesProvider() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProviderResponse response = providerService.setEnabled(1L, true);

            assertThat(testProvider.getState()).isEqualTo(ProviderState.ACTIVE);
            assertThat(response.getState()).isEqualTo("ACTIVE");
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("禁用提供商成功")
        void setEnabled_false_disablesProvider() {
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProviderResponse response = providerService.setEnabled(1L, false);

            assertThat(testProvider.getState()).isEqualTo(ProviderState.DISABLED);
            assertThat(response.getState()).isEqualTo("DISABLED");
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("对不存在的提供商设置启用状态抛出异常")
        void setEnabled_nonExistingProvider_throwsException() {
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.setEnabled(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private Provider createTestProvider(Long id, String providerName, Boolean active) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(providerName);
        provider.setWebsiteUrl("https://example.com");
        provider.setApiDocUrl("https://docs.example.com");
        provider.setPriority(100);
        provider.setState(active ? ProviderState.ACTIVE : ProviderState.DISABLED);
        provider.setCreatedAt(Instant.now());
        provider.setUpdatedAt(Instant.now());
        return provider;
    }
}