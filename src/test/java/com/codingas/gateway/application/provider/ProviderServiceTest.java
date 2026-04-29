package com.codingas.gateway.application.provider;

import com.codingas.gateway.adapter.admin.dto.provider.ProviderCreateRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderQueryRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderResponse;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.entity.Provider.ProviderStatus;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
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

    @InjectMocks
    private ProviderServiceImpl providerService;

    private Provider testProvider;

    @BeforeEach
    void setUp() {
        testProvider = createTestProvider(1L, "OPENAI", "OpenAI", ProviderType.OPENAI, ProviderStatus.ACTIVE);
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建提供商")
    class CreateTests {

        @Test
        @DisplayName("创建提供商成功")
        void create_validRequest_returnsProviderResponse() {
            // given
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderCode("ANTHROPIC");
            request.setProviderName("Anthropic");
            request.setProviderType(ProviderType.ANTHROPIC);
            request.setBaseUrl("https://api.anthropic.com");
            request.setPriority(50);

            when(providerGateway.existsByProviderCode("ANTHROPIC")).thenReturn(false);
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> {
                Provider provider = invocation.getArgument(0);
                provider.setId(2L);
                return provider;
            });

            // when
            ProviderResponse response = providerService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getProviderCode()).isEqualTo("ANTHROPIC");
            assertThat(response.getProviderName()).isEqualTo("Anthropic");
            assertThat(response.getProviderType()).isEqualTo(ProviderType.ANTHROPIC);
            assertThat(response.getPriority()).isEqualTo(50);
            assertThat(response.getStatus()).isEqualTo(ProviderStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();
            verify(providerGateway).existsByProviderCode("ANTHROPIC");
            verify(providerGateway).save(any(Provider.class));
        }

        @Test
        @DisplayName("提供商代码重复时抛出 DuplicateResourceException")
        void create_duplicateProviderCode_throwsException() {
            // given
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderCode("OPENAI");
            request.setProviderName("OpenAI Duplicate");
            request.setProviderType(ProviderType.OPENAI);

            when(providerGateway.existsByProviderCode("OPENAI")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> providerService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining("providerCode");

            verify(providerGateway, never()).save(any(Provider.class));
        }

        @Test
        @DisplayName("创建时未指定优先级则使用默认值100")
        void create_noPriorityProvided_usesDefaultPriority() {
            // given
            ProviderCreateRequest request = new ProviderCreateRequest();
            request.setProviderCode("NEW");
            request.setProviderName("New Provider");
            request.setProviderType(ProviderType.OPENAI);
            // priority 为 null

            when(providerGateway.existsByProviderCode("NEW")).thenReturn(false);
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> {
                Provider provider = invocation.getArgument(0);
                provider.setId(3L);
                return provider;
            });

            // when
            ProviderResponse response = providerService.create(request);

            // then
            assertThat(response.getPriority()).isEqualTo(100);
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取提供商")
    class GetByIdTests {

        @Test
        @DisplayName("提供商存在时返回提供商响应")
        void getById_existingProvider_returnsProviderResponse() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));

            // when
            ProviderResponse response = providerService.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getProviderCode()).isEqualTo("OPENAI");
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
            verify(providerGateway).findById(1L);
        }

        @Test
        @DisplayName("提供商不存在时抛出 ResourceNotFoundException")
        void getById_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> providerService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining("99");
        }
    }

    // ==================== query 测试 ====================

    @Nested
    @DisplayName("query 查询提供商列表")
    class QueryTests {

        @Test
        @DisplayName("无过滤条件时返回所有提供商")
        void query_noFilter_returnsAllProviders() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", ProviderType.ANTHROPIC, ProviderStatus.ACTIVE);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, provider2));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("按关键字过滤提供商")
        void query_withKeyword_filtersProviders() {
            // given
            when(providerGateway.findAll()).thenReturn(List.of(testProvider));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("open");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("关键字按代码或名称匹配")
        void query_keywordMatchesCodeOrName() {
            // given
            Provider provider2 = createTestProvider(2L, "GOOGLE", "Google AI", ProviderType.OPENAI, ProviderStatus.ACTIVE);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, provider2));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("google");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderCode()).isEqualTo("GOOGLE");
        }

        @Test
        @DisplayName("按类型过滤提供商")
        void query_withProviderType_filtersProviders() {
            // given
            when(providerGateway.findAll()).thenReturn(List.of(testProvider));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setProviderType(ProviderType.OPENAI);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderType()).isEqualTo(ProviderType.OPENAI);
        }

        @Test
        @DisplayName("按状态过滤提供商")
        void query_withStatus_filtersProviders() {
            // given
            Provider suspendedProvider = createTestProvider(2L, "SUSPENDED", "Suspended Provider", ProviderType.OPENAI, ProviderStatus.SUSPENDED);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, suspendedProvider));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setStatus(ProviderStatus.SUSPENDED);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ProviderStatus.SUSPENDED);
        }

        @Test
        @DisplayName("分页查询返回正确的数据")
        void query_withPagination_returnsPagedProviders() {
            // given
            List<Provider> providers = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                providers.add(createTestProvider(i, "PROV" + String.format("%03d", i), "Provider " + i, ProviderType.OPENAI, ProviderStatus.ACTIVE));
            }
            when(providerGateway.findAll()).thenReturn(providers);

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setPage(2);
            request.setLimit(10);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(10);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getLimit()).isEqualTo(10);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void query_noMatch_returnsEmptyList() {
            // given
            when(providerGateway.findAll()).thenReturn(List.of(testProvider));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setKeyword("nonexistent");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);
        }

        @Test
        @DisplayName("组合过滤条件")
        void query_combinedFilters_appliesAllFilters() {
            // given
            Provider provider2 = createTestProvider(2L, "ANTHROPIC", "Anthropic", ProviderType.ANTHROPIC, ProviderStatus.ACTIVE);
            Provider suspendedProvider = createTestProvider(3L, "SUSPENDED", "Suspended Provider", ProviderType.ANTHROPIC, ProviderStatus.SUSPENDED);
            when(providerGateway.findAll()).thenReturn(List.of(testProvider, provider2, suspendedProvider));

            ProviderQueryRequest request = new ProviderQueryRequest();
            request.setProviderType(ProviderType.ANTHROPIC);
            request.setStatus(ProviderStatus.ACTIVE);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<ProviderResponse> response = providerService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProviderType()).isEqualTo(ProviderType.ANTHROPIC);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ProviderStatus.ACTIVE);
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新提供商")
    class UpdateTests {

        @Test
        @DisplayName("更新提供商名称成功")
        void update_validName_updatesProvider() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("OpenAI Updated");

            // when
            ProviderResponse response = providerService.update(1L, request);

            // then
            assertThat(response).isNotNull();
            verify(providerGateway).findById(1L);
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("更新提供商类型成功")
        void update_validProviderType_updatesProvider() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderType(ProviderType.ANTHROPIC);

            // when
            ProviderResponse response = providerService.update(1L, request);

            // then
            assertThat(testProvider.getProviderType()).isEqualTo(ProviderType.ANTHROPIC);
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("更新提供商启用状态成功")
        void update_enabledTrue_setsActiveStatus() {
            // given
            testProvider.setStatus(ProviderStatus.SUSPENDED);
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setEnabled(true);

            // when
            ProviderResponse response = providerService.update(1L, request);

            // then
            assertThat(testProvider.getStatus()).isEqualTo(ProviderStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("更新提供商禁用状态成功")
        void update_enabledFalse_setsSuspendedStatus() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setEnabled(false);

            // when
            ProviderResponse response = providerService.update(1L, request);

            // then
            assertThat(testProvider.getStatus()).isEqualTo(ProviderStatus.SUSPENDED);
            assertThat(response.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("更新不存在的提供商抛出异常")
        void update_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            ProviderUpdateRequest request = new ProviderUpdateRequest();
            request.setProviderName("Updated Name");

            // when & then
            assertThatThrownBy(() -> providerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除提供商")
    class DeleteTests {

        @Test
        @DisplayName("删除提供商成功（软删除）")
        void delete_existingProvider_softDeletes() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenReturn(testProvider);

            // when
            providerService.delete(1L);

            // then
            assertThat(testProvider.getDeletedAt()).isNotNull();
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("删除不存在的提供商抛出异常")
        void delete_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> providerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== setEnabled 测试 ====================

    @Nested
    @DisplayName("setEnabled 启用/禁用提供商")
    class SetEnabledTests {

        @Test
        @DisplayName("启用提供商成功")
        void setEnabled_true_enablesProvider() {
            // given
            testProvider.setStatus(ProviderStatus.SUSPENDED);
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ProviderResponse response = providerService.setEnabled(1L, true);

            // then
            assertThat(testProvider.getStatus()).isEqualTo(ProviderStatus.ACTIVE);
            assertThat(response.getEnabled()).isTrue();
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("禁用提供商成功")
        void setEnabled_false_disablesProvider() {
            // given
            when(providerGateway.findById(1L)).thenReturn(Optional.of(testProvider));
            when(providerGateway.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ProviderResponse response = providerService.setEnabled(1L, false);

            // then
            assertThat(testProvider.getStatus()).isEqualTo(ProviderStatus.SUSPENDED);
            assertThat(response.getEnabled()).isFalse();
            verify(providerGateway).save(testProvider);
        }

        @Test
        @DisplayName("对不存在的提供商设置启用状态抛出异常")
        void setEnabled_nonExistingProvider_throwsException() {
            // given
            when(providerGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> providerService.setEnabled(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== 辅助方法 ====================

    private Provider createTestProvider(Long id, String providerCode, String providerName,
                                        ProviderType providerType, ProviderStatus status) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setProviderCode(providerCode);
        provider.setProviderName(providerName);
        provider.setProviderType(providerType);
        provider.setBaseUrl("https://api." + providerCode.toLowerCase() + ".com");
        provider.setWebsiteUrl("https://" + providerCode.toLowerCase() + ".com");
        provider.setApiDocUrl("https://docs." + providerCode.toLowerCase() + ".com");
        provider.setPriority(100);
        provider.setStatus(status);
        provider.setCreatedAt(Instant.now());
        provider.setUpdatedAt(Instant.now());
        return provider;
    }
}