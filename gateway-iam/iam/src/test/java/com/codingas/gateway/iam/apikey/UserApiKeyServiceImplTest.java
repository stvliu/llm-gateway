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
package com.codingas.gateway.iam.apikey;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.iam.application.ApplicationRepository;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UserApiKeyManagerImpl 单元测试
 *
 * <p>owner 校验：默认用例为管理员上下文（hasRole(ADMIN)=true）；
 * 普通用户上下文（hasRole=false + getLoginIdAsLong）用于归属校验用例。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserApiKeyManagerImpl 测试")
class UserApiKeyManagerImplTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private UserApiKeyGenerator userApiKeyGenerator;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private UserApiKeyManagerImpl service;

    private static final Long USER_ID = 50L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long APPLICATION_ID = 7L;
    private static final Long API_KEY_ID = 100L;

    /** 管理员上下文（对 Key 操作无归属限制） */
    private MockedStatic<StpUtil> stubAdmin() {
        MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
        stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(true);
        return stp;
    }

    /** 普通用户上下文（登录用户 = userId） */
    private MockedStatic<StpUtil> stubUser(Long userId) {
        MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
        stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(false);
        stp.when(() -> StpUtil.getLoginIdAsLong()).thenReturn(userId);
        return stp;
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("管理员创建密钥成功 — applicationId 落库")
        void create_success_setsApplicationId() {
            GeneratedApiKey generated = new GeneratedApiKey("sk-abc1xxxxx", "sk-abc1");
            when(userApiKeyGenerator.generate()).thenReturn(generated);
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationRepository.findById(APPLICATION_ID)).thenReturn(app);

            UserApiKey saved = createSampleApiKey();
            saved.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.save(any(UserApiKey.class))).thenReturn(saved);

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKey(
                        USER_ID, APPLICATION_ID, "test-key"
                );
                UserApiKey response = service.create(request);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(API_KEY_ID);
                assertThat(response.getKeyPlain()).startsWith("sk-");
                verify(userApiKeyRepository).save(argThat(key ->
                        key.getUserId().equals(USER_ID)
                                && key.getApplicationId().equals(APPLICATION_ID)
                ));
            }
        }

        @Test
        @DisplayName("普通用户创建密钥 — 强制归属当前用户（忽略请求体 userId）")
        void create_userRole_forcesOwnUserId() {
            GeneratedApiKey generated = new GeneratedApiKey("sk-abc1xxxxx", "sk-abc1");
            when(userApiKeyGenerator.generate()).thenReturn(generated);
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationRepository.findById(APPLICATION_ID)).thenReturn(app);
            when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                // 请求体尝试指定他人 userId，必须被强制覆盖为当前用户
                UserApiKey request = apiKey(
                        OTHER_USER_ID, APPLICATION_ID, "test-key"
                );
                UserApiKey response = service.create(request);

                assertThat(response).isNotNull();
                verify(userApiKeyRepository).save(argThat(key ->
                        key.getUserId().equals(USER_ID)
                ));
            }
        }

        @Test
        @DisplayName("applicationId 引用不存在 — 抛 GatewayRequestException")
        void create_applicationNotFound_throws() {
            when(applicationRepository.findById(APPLICATION_ID)).thenReturn(null);

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKey(
                        USER_ID, APPLICATION_ID, "test-key"
                );
                assertThatThrownBy(() -> service.create(request))
                        .isInstanceOf(GatewayRequestException.class)
                        .hasMessageContaining("应用不存在");
                verify(userApiKeyRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("ApiKeyGenerator 碰撞超限抛异常时，create 也抛异常")
        void create_generatorFails_throwsException() {
            Application app = new Application();
            app.setId(APPLICATION_ID);
            when(applicationRepository.findById(APPLICATION_ID)).thenReturn(app);
            when(userApiKeyGenerator.generate())
                    .thenThrow(new IllegalStateException("无法生成唯一的 API Key，请重试"));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKey(
                        USER_ID, APPLICATION_ID, "test-key"
                );
                assertThatThrownBy(() -> service.create(request))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("无法生成唯一的 API Key");
            }
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("管理员补绑 applicationId — 校验存在并落库")
        void update_rebindApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            Application app = new Application();
            app.setId(99L);
            when(applicationRepository.findById(99L)).thenReturn(app);
            when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKeyUpdate(99L, null);
                UserApiKey response = service.update(API_KEY_ID, request);

                assertThat(response).isNotNull();
                assertThat(response.getApplicationId()).isEqualTo(99L);
                verify(userApiKeyRepository).save(argThat(key -> key.getApplicationId().equals(99L)));
            }
        }

        @Test
        @DisplayName("管理员补绑 applicationId 引用不存在 — 抛 GatewayRequestException")
        void update_applicationNotFound_throws() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(applicationRepository.findById(99L)).thenReturn(null);

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKeyUpdate(99L, null);
                assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                        .isInstanceOf(GatewayRequestException.class)
                        .hasMessageContaining("应用不存在");
            }
        }

        @Test
        @DisplayName("管理员仅更新名称 — applicationId 不变")
        void update_nameOnly() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKeyUpdate(null, "new-name");
                UserApiKey response = service.update(API_KEY_ID, request);

                assertThat(response.getName()).isEqualTo("new-name");
                assertThat(response.getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }

        @Test
        @DisplayName("普通用户更新他人 Key — 抛 ForbiddenException")
        void update_otherUserKey_forbidden() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setUserId(OTHER_USER_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.update(API_KEY_ID, apiKeyUpdate(null, "x")))
                        .isInstanceOf(ForbiddenException.class);
                verify(userApiKeyRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void update_notFound() {
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.empty());

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey request = apiKeyUpdate(null, "updated");
                assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("findByApplicationId 方法测试")
    class FindByApplicationIdTests {

        @Test
        @DisplayName("管理员查询应用下的所有 Key")
        void findByApplicationId_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                List<UserApiKey> responses = service.findByApplicationId(APPLICATION_ID);

                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }

        @Test
        @DisplayName("普通用户查询应用 Key — 抛 ForbiddenException")
        void findByApplicationId_userRole_forbidden() {
            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.findByApplicationId(APPLICATION_ID))
                        .isInstanceOf(ForbiddenException.class);
                verify(userApiKeyRepository, never()).findByApplicationId(any());
            }
        }

        @Test
        @DisplayName("应用下无 Key 返回空列表")
        void findByApplicationId_empty() {
            when(userApiKeyRepository.findByApplicationId(APPLICATION_ID)).thenReturn(List.of());

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                List<UserApiKey> responses = service.findByApplicationId(APPLICATION_ID);

                assertThat(responses).isEmpty();
            }
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
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey response = service.getById(API_KEY_ID);

                assertThat(response.getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }
    }

    @Nested
    @DisplayName("findAllNonDeleted 方法测试")
    class FindAllNonDeletedTests {

        @Test
        @DisplayName("管理员返回全部未删除 Key")
        void findAllNonDeleted_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.findAllNonDeleted()).thenReturn(List.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                List<UserApiKey> responses = service.findAllNonDeleted();

                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }

        @Test
        @DisplayName("普通用户查询全部 Key — 抛 ForbiddenException")
        void findAllNonDeleted_userRole_forbidden() {
            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.findAllNonDeleted())
                        .isInstanceOf(ForbiddenException.class);
                verify(userApiKeyRepository, never()).findAllNonDeleted();
            }
        }

        @Test
        @DisplayName("无未删除 Key 返回空列表")
        void findAllNonDeleted_empty() {
            when(userApiKeyRepository.findAllNonDeleted()).thenReturn(List.of());

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                assertThat(service.findAllNonDeleted()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("管理员返回用户的 Key 列表含 applicationId（回归保护）")
        void findByUserId_success() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.findByUserId(USER_ID)).thenReturn(List.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                List<UserApiKey> responses = service.findByUserId(USER_ID);

                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }

        @Test
        @DisplayName("普通用户按 userId 查询 — 抛 ForbiddenException")
        void findByUserId_userRole_forbidden() {
            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.findByUserId(OTHER_USER_ID))
                        .isInstanceOf(ForbiddenException.class);
                verify(userApiKeyRepository, never()).findByUserId(any());
            }
        }

        @Test
        @DisplayName("普通用户查询自己的 Key 列表 — 允许（MeController 链路）")
        void findByUserId_ownKeys_ok() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findByUserId(USER_ID)).thenReturn(List.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                List<UserApiKey> responses = service.findByUserId(USER_ID);

                assertThat(responses).hasSize(1);
                assertThat(responses.get(0).getUserId()).isEqualTo(USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("getDetailById 方法测试")
    class GetDetailByIdTests {

        @Test
        @DisplayName("管理员详情响应含 applicationId（覆盖 spec scenario 4.2）")
        void getDetailById_responseContainsApplicationId() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setApplicationId(APPLICATION_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                UserApiKey response = service.getDetailById(API_KEY_ID);

                assertThat(response).isNotNull();
                assertThat(response.getApplicationId()).isEqualTo(APPLICATION_ID);
            }
        }

        @Test
        @DisplayName("普通用户查看他人 Key 详情 — 抛 ForbiddenException")
        void getDetailById_otherUserKey_forbidden() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setUserId(OTHER_USER_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.getDetailById(API_KEY_ID))
                        .isInstanceOf(ForbiddenException.class);
            }
        }

        @Test
        @DisplayName("普通用户查看自己的 Key 详情 — 允许（Quickstart 链路）")
        void getDetailById_ownKey_ok() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                UserApiKey response = service.getDetailById(API_KEY_ID);

                assertThat(response).isNotNull();
                assertThat(response.getUserId()).isEqualTo(USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("getById 归属与 notFound 测试")
    class GetByIdTests {

        @Test
        @DisplayName("普通用户查看自己的 Key — 允许")
        void getById_ownKey_ok() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                UserApiKey response = service.getById(API_KEY_ID);

                assertThat(response).isNotNull();
                assertThat(response.getUserId()).isEqualTo(USER_ID);
            }
        }

        @Test
        @DisplayName("普通用户查看他人 Key — 抛 ForbiddenException")
        void getById_otherUserKey_forbidden() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setUserId(OTHER_USER_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.getById(API_KEY_ID))
                        .isInstanceOf(ForbiddenException.class);
            }
        }

        @Test
        @DisplayName("密钥不存在 — 抛 IllegalArgumentException（回归保护）")
        void getById_notFound() {
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.empty());

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                assertThatThrownBy(() -> service.getById(API_KEY_ID))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("API Key 不存在");
            }
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("管理员删除密钥 — 调用 gateway.delete（回归保护）")
        void delete_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubAdmin()) {
                service.delete(API_KEY_ID);

                verify(userApiKeyRepository).delete(apiKey);
            }
        }

        @Test
        @DisplayName("普通用户删除他人 Key — 抛 ForbiddenException")
        void delete_otherUserKey_forbidden() {
            UserApiKey apiKey = createSampleApiKey();
            apiKey.setUserId(OTHER_USER_ID);
            when(userApiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

            try (MockedStatic<StpUtil> stp = stubUser(USER_ID)) {
                assertThatThrownBy(() -> service.delete(API_KEY_ID))
                        .isInstanceOf(ForbiddenException.class);
                verify(userApiKeyRepository, never()).delete(any());
            }
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

    /** 构造 Key 实体（create 用：userId/applicationId/name） */
    private UserApiKey apiKey(Long userId, Long applicationId, String name) {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(userId);
        apiKey.setApplicationId(applicationId);
        apiKey.setName(name);
        return apiKey;
    }

    /** 构造 Key 实体（update 用：applicationId/name，null 表示不更新） */
    private UserApiKey apiKeyUpdate(Long applicationId, String name) {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setApplicationId(applicationId);
        apiKey.setName(name);
        return apiKey;
    }
}
