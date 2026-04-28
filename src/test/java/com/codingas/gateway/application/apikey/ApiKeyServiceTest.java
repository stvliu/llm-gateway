package com.codingas.gateway.application.apikey;

import com.codingas.gateway.adapter.admin.dto.apikey.ApiKeyCreateRequest;
import com.codingas.gateway.adapter.admin.dto.apikey.ApiKeyQueryRequest;
import com.codingas.gateway.adapter.admin.dto.apikey.ApiKeyResponse;
import com.codingas.gateway.adapter.admin.dto.apikey.ApiKeyUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.GatewayApiKey.ApiKeyStatus;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ApiKeyService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyGateway apiKeyGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private ApiKeyEncryptionService encryptionService;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    @Nested
    @DisplayName("create(ApiKeyCreateRequest) 测试")
    class CreateTests {

        @Test
        @DisplayName("当用户存在时，创建 API Key 成功")
        void create_userExists_savesApiKey() {
            // given
            ApiKeyCreateRequest request = new ApiKeyCreateRequest();
            request.setUserId(1L);
            request.setName("Test API Key");

            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(encryptionService.hashKey(any())).thenReturn("hashed-key");
            when(apiKeyGateway.save(any())).thenAnswer(invocation -> {
                GatewayApiKey saved = invocation.getArgument(0);
                saved.setId(100L);
                saved.setCreatedAt(Instant.now());
                saved.setUpdatedAt(Instant.now());
                return saved;
            });

            // when
            ApiKeyResponse response = apiKeyService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getName()).isEqualTo("Test API Key");
            assertThat(response.getRawKey()).startsWith("sk-");
            assertThat(response.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);

            // 验证用户查找
            verify(userGateway).findById(1L);
            // 验证加密服务调用
            verify(encryptionService).hashKey(any());
            // 验证保存
            ArgumentCaptor<GatewayApiKey> captor = ArgumentCaptor.forClass(GatewayApiKey.class);
            verify(apiKeyGateway).save(captor.capture());

            GatewayApiKey savedKey = captor.getValue();
            assertThat(savedKey.getKeyCode()).startsWith("sk-");
            assertThat(savedKey.getName()).isEqualTo("Test API Key");
            assertThat(savedKey.getUser()).isEqualTo(user);
            assertThat(savedKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        }

        @Test
        @DisplayName("当用户不存在时，抛出 ResourceNotFoundException")
        void create_userNotFound_throwsException() {
            // given
            ApiKeyCreateRequest request = new ApiKeyCreateRequest();
            request.setUserId(999L);
            request.setName("Test API Key");

            when(userGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> apiKeyService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("999");

            verify(userGateway).findById(999L);
            verify(apiKeyGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById(Long id) 测试")
    class GetByIdTests {

        @Test
        @DisplayName("当 API Key 存在时，返回 API Key 响应")
        void getById_exists_returnsResponse() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey apiKey = new GatewayApiKey();
            apiKey.setId(100L);
            apiKey.setKeyCode("sk-test-key");
            apiKey.setKeyHash("hashed");
            apiKey.setUser(user);
            apiKey.setName("Test Key");
            apiKey.setStatus(ApiKeyStatus.ACTIVE);
            apiKey.setCreatedAt(Instant.now());
            apiKey.setUpdatedAt(Instant.now());

            when(apiKeyGateway.findById(100L)).thenReturn(Optional.of(apiKey));

            // when
            ApiKeyResponse response = apiKeyService.getById(100L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getKeyCode()).isEqualTo("sk-test-key");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getName()).isEqualTo("Test Key");
            assertThat(response.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);

            verify(apiKeyGateway).findById(100L);
        }

        @Test
        @DisplayName("当 API Key 不存在时，抛出 ResourceNotFoundException")
        void getById_notFound_throwsException() {
            // given
            when(apiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> apiKeyService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApiKey")
                .hasMessageContaining("999");

            verify(apiKeyGateway).findById(999L);
        }
    }

    @Nested
    @DisplayName("query(ApiKeyQueryRequest) 测试")
    class QueryTests {

        @Test
        @DisplayName("当无查询条件时，返回所有 API Key")
        void query_noFilters_returnsAllApiKeys() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey apiKey1 = new GatewayApiKey();
            apiKey1.setId(1L);
            apiKey1.setKeyCode("sk-key-1");
            apiKey1.setKeyHash("hash1");
            apiKey1.setUser(user);
            apiKey1.setName("Key 1");
            apiKey1.setStatus(ApiKeyStatus.ACTIVE);
            apiKey1.setCreatedAt(Instant.now());
            apiKey1.setUpdatedAt(Instant.now());

            GatewayApiKey apiKey2 = new GatewayApiKey();
            apiKey2.setId(2L);
            apiKey2.setKeyCode("sk-key-2");
            apiKey2.setKeyHash("hash2");
            apiKey2.setUser(user);
            apiKey2.setName("Key 2");
            apiKey2.setStatus(ApiKeyStatus.ACTIVE);
            apiKey2.setCreatedAt(Instant.now());
            apiKey2.setUpdatedAt(Instant.now());

            ApiKeyQueryRequest request = new ApiKeyQueryRequest();

            when(apiKeyGateway.findAll()).thenReturn(List.of(apiKey1, apiKey2));

            // when
            PageResponse<ApiKeyResponse> response = apiKeyService.query(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);

            verify(apiKeyGateway).findAll();
        }

        @Test
        @DisplayName("当有关键字过滤时，按关键字搜索")
        void query_withKeyword_filtersResults() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey apiKey1 = new GatewayApiKey();
            apiKey1.setId(1L);
            apiKey1.setKeyCode("sk-search-me");
            apiKey1.setKeyHash("hash1");
            apiKey1.setUser(user);
            apiKey1.setName("Search Key");
            apiKey1.setStatus(ApiKeyStatus.ACTIVE);
            apiKey1.setCreatedAt(Instant.now());
            apiKey1.setUpdatedAt(Instant.now());

            GatewayApiKey apiKey2 = new GatewayApiKey();
            apiKey2.setId(2L);
            apiKey2.setKeyCode("sk-other-key");
            apiKey2.setKeyHash("hash2");
            apiKey2.setUser(user);
            apiKey2.setName("Other Key");
            apiKey2.setStatus(ApiKeyStatus.ACTIVE);
            apiKey2.setCreatedAt(Instant.now());
            apiKey2.setUpdatedAt(Instant.now());

            ApiKeyQueryRequest request = new ApiKeyQueryRequest();
            request.setKeyword("search");

            when(apiKeyGateway.findAll()).thenReturn(List.of(apiKey1, apiKey2));

            // when
            PageResponse<ApiKeyResponse> response = apiKeyService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getName()).isEqualTo("Search Key");

            verify(apiKeyGateway).findAll();
        }

        @Test
        @DisplayName("当按用户 ID 过滤时，只返回该用户的 API Key")
        void query_withUserId_filtersByUserId() {
            // given
            User user1 = new User();
            user1.setId(1L);
            user1.setUserCode("user-001");
            user1.setUsername("user1");

            User user2 = new User();
            user2.setId(2L);
            user2.setUserCode("user-002");
            user2.setUsername("user2");

            GatewayApiKey apiKey1 = new GatewayApiKey();
            apiKey1.setId(1L);
            apiKey1.setKeyCode("sk-key-1");
            apiKey1.setKeyHash("hash1");
            apiKey1.setUser(user1);
            apiKey1.setName("User1 Key");
            apiKey1.setStatus(ApiKeyStatus.ACTIVE);
            apiKey1.setCreatedAt(Instant.now());
            apiKey1.setUpdatedAt(Instant.now());

            GatewayApiKey apiKey2 = new GatewayApiKey();
            apiKey2.setId(2L);
            apiKey2.setKeyCode("sk-key-2");
            apiKey2.setKeyHash("hash2");
            apiKey2.setUser(user2);
            apiKey2.setName("User2 Key");
            apiKey2.setStatus(ApiKeyStatus.ACTIVE);
            apiKey2.setCreatedAt(Instant.now());
            apiKey2.setUpdatedAt(Instant.now());

            ApiKeyQueryRequest request = new ApiKeyQueryRequest();
            request.setUserId(1L);

            when(apiKeyGateway.findAll()).thenReturn(List.of(apiKey1, apiKey2));

            // when
            PageResponse<ApiKeyResponse> response = apiKeyService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getUserId()).isEqualTo(1L);

            verify(apiKeyGateway).findAll();
        }

        @Test
        @DisplayName("当按状态过滤时，只返回该状态的 API Key")
        void query_withStatus_filtersByStatus() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey activeKey = new GatewayApiKey();
            activeKey.setId(1L);
            activeKey.setKeyCode("sk-active-key");
            activeKey.setKeyHash("hash1");
            activeKey.setUser(user);
            activeKey.setName("Active Key");
            activeKey.setStatus(ApiKeyStatus.ACTIVE);
            activeKey.setCreatedAt(Instant.now());
            activeKey.setUpdatedAt(Instant.now());

            GatewayApiKey disabledKey = new GatewayApiKey();
            disabledKey.setId(2L);
            disabledKey.setKeyCode("sk-disabled-key");
            disabledKey.setKeyHash("hash2");
            disabledKey.setUser(user);
            disabledKey.setName("Disabled Key");
            disabledKey.setStatus(ApiKeyStatus.DISABLED);
            disabledKey.setCreatedAt(Instant.now());
            disabledKey.setUpdatedAt(Instant.now());

            ApiKeyQueryRequest request = new ApiKeyQueryRequest();
            request.setStatus(ApiKeyStatus.ACTIVE);

            when(apiKeyGateway.findAll()).thenReturn(List.of(activeKey, disabledKey));

            // when
            PageResponse<ApiKeyResponse> response = apiKeyService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);

            verify(apiKeyGateway).findAll();
        }

        @Test
        @DisplayName("当无结果时，返回空列表")
        void query_noResults_returnsEmptyList() {
            // given
            ApiKeyQueryRequest request = new ApiKeyQueryRequest();
            request.setKeyword("nonexistent");

            when(apiKeyGateway.findAll()).thenReturn(Collections.emptyList());

            // when
            PageResponse<ApiKeyResponse> response = apiKeyService.query(request);

            // then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);

            verify(apiKeyGateway).findAll();
        }
    }

    @Nested
    @DisplayName("update(Long id, ApiKeyUpdateRequest) 测试")
    class UpdateTests {

        @Test
        @DisplayName("当 API Key 存在时，更新成功")
        void update_exists_updatesApiKey() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey existingKey = new GatewayApiKey();
            existingKey.setId(100L);
            existingKey.setKeyCode("sk-old-key");
            existingKey.setKeyHash("old-hash");
            existingKey.setUser(user);
            existingKey.setName("Old Name");
            existingKey.setStatus(ApiKeyStatus.ACTIVE);
            existingKey.setCreatedAt(Instant.now());
            existingKey.setUpdatedAt(Instant.now());

            ApiKeyUpdateRequest request = new ApiKeyUpdateRequest();
            request.setName("New Name");
            request.setEnabled(false);

            when(apiKeyGateway.findById(100L)).thenReturn(Optional.of(existingKey));
            when(apiKeyGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ApiKeyResponse response = apiKeyService.update(100L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("New Name");
            assertThat(response.getStatus()).isEqualTo(ApiKeyStatus.DISABLED);

            verify(apiKeyGateway).findById(100L);
            verify(apiKeyGateway).save(existingKey);
        }

        @Test
        @DisplayName("当 API Key 不存在时，抛出 ResourceNotFoundException")
        void update_notFound_throwsException() {
            // given
            ApiKeyUpdateRequest request = new ApiKeyUpdateRequest();
            request.setName("New Name");

            when(apiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> apiKeyService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApiKey")
                .hasMessageContaining("999");

            verify(apiKeyGateway).findById(999L);
            verify(apiKeyGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete(Long id) 测试")
    class DeleteTests {

        @Test
        @DisplayName("当 API Key 存在时，执行软删除")
        void delete_exists_softDeletesApiKey() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey existingKey = new GatewayApiKey();
            existingKey.setId(100L);
            existingKey.setKeyCode("sk-to-delete");
            existingKey.setKeyHash("hash");
            existingKey.setUser(user);
            existingKey.setStatus(ApiKeyStatus.ACTIVE);
            existingKey.setCreatedAt(Instant.now());

            when(apiKeyGateway.findById(100L)).thenReturn(Optional.of(existingKey));
            when(apiKeyGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            apiKeyService.delete(100L);

            // then
            assertThat(existingKey.getDeletedAt()).isNotNull();

            verify(apiKeyGateway).findById(100L);
            verify(apiKeyGateway).save(existingKey);
        }

        @Test
        @DisplayName("当 API Key 不存在时，抛出 ResourceNotFoundException")
        void delete_notFound_throwsException() {
            // given
            when(apiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> apiKeyService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApiKey")
                .hasMessageContaining("999");

            verify(apiKeyGateway).findById(999L);
            verify(apiKeyGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("setEnabled(Long id, boolean enabled) 测试")
    class SetEnabledTests {

        @Test
        @DisplayName("当启用 API Key 时，状态变为 ACTIVE")
        void setEnabled_true_setsStatusToActive() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey existingKey = new GatewayApiKey();
            existingKey.setId(100L);
            existingKey.setKeyCode("sk-key");
            existingKey.setKeyHash("hash");
            existingKey.setUser(user);
            existingKey.setStatus(ApiKeyStatus.DISABLED);
            existingKey.setCreatedAt(Instant.now());
            existingKey.setUpdatedAt(Instant.now());

            when(apiKeyGateway.findById(100L)).thenReturn(Optional.of(existingKey));
            when(apiKeyGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ApiKeyResponse response = apiKeyService.setEnabled(100L, true);

            // then
            assertThat(response.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);

            verify(apiKeyGateway).findById(100L);
            verify(apiKeyGateway).save(existingKey);
        }

        @Test
        @DisplayName("当禁用 API Key 时，状态变为 DISABLED")
        void setEnabled_false_setsStatusToDisabled() {
            // given
            User user = new User();
            user.setId(1L);
            user.setUserCode("user-001");
            user.setUsername("testuser");

            GatewayApiKey existingKey = new GatewayApiKey();
            existingKey.setId(100L);
            existingKey.setKeyCode("sk-key");
            existingKey.setKeyHash("hash");
            existingKey.setUser(user);
            existingKey.setStatus(ApiKeyStatus.ACTIVE);
            existingKey.setCreatedAt(Instant.now());
            existingKey.setUpdatedAt(Instant.now());

            when(apiKeyGateway.findById(100L)).thenReturn(Optional.of(existingKey));
            when(apiKeyGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            ApiKeyResponse response = apiKeyService.setEnabled(100L, false);

            // then
            assertThat(response.getStatus()).isEqualTo(ApiKeyStatus.DISABLED);

            verify(apiKeyGateway).findById(100L);
            verify(apiKeyGateway).save(existingKey);
        }

        @Test
        @DisplayName("当 API Key 不存在时，抛出 ResourceNotFoundException")
        void setEnabled_notFound_throwsException() {
            // given
            when(apiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> apiKeyService.setEnabled(999L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApiKey")
                .hasMessageContaining("999");

            verify(apiKeyGateway).findById(999L);
            verify(apiKeyGateway, never()).save(any());
        }
    }
}