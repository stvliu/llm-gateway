/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iam.service;

import com.codingas.gateway.iam.dto.*;
import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.iam.application.ApplicationGateway;
import com.codingas.gateway.iam.apikey.UserApiKeyGenerator;
import com.codingas.gateway.iam.apikey.GeneratedApiKey;
import com.codingas.gateway.iam.apikey.UserApiKey;
import com.codingas.gateway.iam.apikey.UserApiKeyGateway;
import com.codingas.gateway.common.exception.GatewayRequestException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UserApiKeyServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserApiKeyServiceImpl 测试")
class UserApiKeyServiceImplTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private UserApiKeyGenerator userApiKeyGenerator;

    @Mock
    private ApplicationGateway applicationGateway;

    @InjectMocks
    private UserApiKeyServiceImpl service;

    private static final Long USER_ID = 50L;
    private static final Long APPLICATION_ID = 7L;
    private static final Long API_KEY_ID = 100L;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建密钥成功 — applicationId 落库")
        void create_success_setsApplicationId() {
            GeneratedApiKey generated = new GeneratedApiKey("sk-abc1xxxxx", "sk-abc1");
            when(userApiKeyGenerator.generate()).thenReturn(generated);
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(app);

            UserApiKey saved = createSampleApiKey();
            saved.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            UserApiKeyCreateResponse response = service.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(API_KEY_ID);
            assertThat(response.apiKeyPlain()).startsWith("sk-");
            verify(userApiKeyGateway).save(argThat(key ->
                    key.getUserId().equals(USER_ID)
                            && key.getApplicationId().equals(APPLICATION_ID)
            ));
        }

        @Test
        @DisplayName("applicationId 引用不存在 — 抛 GatewayRequestException")
        void create_applicationNotFound_throws() {
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(null);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("应用不存在");
            verify(userApiKeyGateway, never()).save(any());
        }

        @Test
        @DisplayName("ApiKeyGenerator 碰撞超限抛异常时，create 也抛异常")
        void create_generatorFails_throwsException() {
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationGateway.findById(APPLICATION_ID)).thenReturn(app);
            when(userApiKeyGenerator.generate())
                    .thenThrow(new IllegalStateException("无法生成唯一的 API Key，请重试"));

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, APPLICATION_ID, "test-key"
            );
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成唯一的 API Key");
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("补绑 applicationId — 校验存在并落库")
        void update_rebindApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            Application app = new Application();
            app.setId(99L);
            when(applicationGateway.findById(99L)).thenReturn(app);
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(99L, null);
            UserApiKeyResponse response = service.update(API_KEY_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.applicationId()).isEqualTo(99L);
            verify(userApiKeyGateway).save(argThat(key -> key.getApplicationId().equals(99L)));
        }

        @Test
        @DisplayName("补绑 applicationId 引用不存在 — 抛 GatewayRequestException")
        void update_applicationNotFound_throws() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(applicationGateway.findById(99L)).thenReturn(null);

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(99L, null);
            assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("应用不存在");
        }

        @Test
        @DisplayName("仅更新名称 — applicationId 不变")
        void update_nameOnly() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(null, "new-name");
            UserApiKeyResponse response = service.update(API_KEY_ID, request);

            assertThat(response.name()).isEqualTo("new-name");
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void update_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(null, "updated");
            assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findByApplicationId 方法测试")
    class FindByApplicationIdTests {

        @Test
        @DisplayName("查询应用下的所有 Key")
        void findByApplicationId_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(apiKey));

            List<UserApiKeyResponse> responses = service.findByApplicationId(APPLICATION_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("应用下无 Key 返回空列表")
        void findByApplicationId_empty() {
            when(userApiKeyGateway.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

            List<UserApiKeyResponse> responses = service.findByApplicationId(APPLICATION_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("响应映射测试")
    class ResponseMappingTests {

        @Test
        @DisplayName("toResponse 含 applicationId")
        void getById_responseContainsApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            UserApiKeyResponse response = service.getById(API_KEY_ID);

            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }
    }

    @Nested
    @DisplayName("findAllNonDeleted 方法测试")
    class FindAllNonDeletedTests {

        @Test
        @DisplayName("返回全部未删除 Key")
        void findAllNonDeleted_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findAllNonDeleted()).thenReturn(List.of(apiKey));

            List<UserApiKeyResponse> responses = service.findAllNonDeleted();

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).applicationId()).isEqualTo(APPLICATION_ID);
        }

        @Test
        @DisplayName("无未删除 Key 返回空列表")
        void findAllNonDeleted_empty() {
            when(userApiKeyGateway.findAllNonDeleted()).thenReturn(List.of());

            assertThat(service.findAllNonDeleted()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("返回用户的 Key 列表含 applicationId（回归保护）")
        void findByUserId_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findByUserId(USER_ID)).thenReturn(List.of(apiKey));

            List<UserApiKeyResponse> responses = service.findByUserId(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).applicationId()).isEqualTo(APPLICATION_ID);
        }
    }

    @Nested
    @DisplayName("getDetailById 方法测试")
    class GetDetailByIdTests {

        @Test
        @DisplayName("详情响应含 applicationId（覆盖 spec scenario 4.2）")
        void getDetailById_responseContainsApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            UserApiKeyDetailResponse response = service.getDetailById(API_KEY_ID);

            assertThat(response).isNotNull();
            assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        }
    }

    @Nested
    @DisplayName("getById notFound 测试")
    class GetByIdNotFoundTests {

        @Test
        @DisplayName("密钥不存在 — 抛 IllegalArgumentException（回归保护）")
        void getById_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(API_KEY_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("API Key 不存在");
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除密钥 — 调用 gateway.delete（回归保护）")
        void delete_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            service.delete(API_KEY_ID);

            verify(userApiKeyGateway).delete(apiKey);
        }
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(API_KEY_ID);
        apiKey.setUserId(USER_ID);
        apiKey.setKeyPlain("sk-abc1xxxxx");
        apiKey.setKeyPrefix("sk-abc1");
        apiKey.setName("test-key");
        return apiKey;
    }
}
