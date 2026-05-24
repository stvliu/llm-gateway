package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.*;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.iam.service.UserApiKeyGenerator;
import com.codingas.gateway.domain.iam.service.GeneratedApiKey;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
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
    private ChannelGateway channelGateway;

    @Mock
    private UserApiKeyGenerator userApiKeyGenerator;

    @InjectMocks
    private UserApiKeyServiceImpl service;

    private static final Long USER_ID = 50L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建密钥成功")
        void create_success() {
            GeneratedApiKey generated = new GeneratedApiKey("sk-abc1xxxxx", "sk-abc1");
            when(userApiKeyGenerator.generate()).thenReturn(generated);

            UserApiKey saved = createSampleApiKey();
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
            );
            UserApiKeyCreateResponse response = service.create(request);

            assertThat(response).isNotNull();
            assertThat(response.apiKeyPlain()).startsWith("sk-");
            assertThat(response.id()).isEqualTo(API_KEY_ID);
            verify(userApiKeyGateway).save(argThat(key ->
                    key.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("ApiKeyGenerator 碰撞超限抛异常时，create 也抛异常")
        void create_generatorFails_throwsException() {
            when(userApiKeyGenerator.generate())
                    .thenThrow(new IllegalStateException("无法生成唯一的 API Key，请重试"));

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
            );
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成唯一的 API Key");
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("查询用户的所有密钥")
        void findByUserId_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findByUserId(USER_ID)).thenReturn(List.of(apiKey));
            mockProductBriefs();

            List<UserApiKeyResponse> responses = service.findByUserId(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("用户没有密钥时返回空列表")
        void findByUserId_emptyList() {
            when(userApiKeyGateway.findByUserId(USER_ID)).thenReturn(List.of());

            List<UserApiKeyResponse> responses = service.findByUserId(USER_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("查询存在的密钥")
        void getById_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            mockProductBriefs();

            UserApiKeyResponse response = service.getById(API_KEY_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(API_KEY_ID);
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void getById_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(API_KEY_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getDetailById 方法测试")
    class GetDetailByIdTests {

        @Test
        @DisplayName("查询密钥详情（含明文）")
        void getDetailById_success() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            mockProductBriefs();

            UserApiKeyDetailResponse response = service.getDetailById(API_KEY_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(API_KEY_ID);
            assertThat(response.keyPlain()).isEqualTo("sk-abc1xxxxx");
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void getDetailById_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getDetailById(API_KEY_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新密钥名称和模型")
        void update_nameAndModels() {
            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));
            mockProductBriefs();

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                    "updated-name", List.of(PRODUCT_ID), List.of("claude-3-5-sonnet"), null, null
            );
            UserApiKeyResponse response = service.update(API_KEY_ID, request);

            assertThat(response).isNotNull();
            verify(userApiKeyGateway).save(argThat(key ->
                    "updated-name".equals(key.getName()) &&
                    key.getModels().equals(List.of("claude-3-5-sonnet"))
            ));
        }

        @Test
        @DisplayName("密钥不存在 — 抛异常")
        void update_notFound() {
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.empty());

            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                    "updated", null, null, null, null
            );
            assertThatThrownBy(() -> service.update(API_KEY_ID, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除密钥 — 先查找再删除")
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
        apiKey.setChannelIds(List.of(PRODUCT_ID));
        apiKey.setKeyPlain("sk-abc1xxxxx");
        apiKey.setKeyPrefix("sk-abc1");
        apiKey.setName("test-key");
        apiKey.setModels(List.of("gpt-4o"));
        apiKey.setQuotaLimit(100000L);
        apiKey.setState(UserApiKeyState.ACTIVE);
        return apiKey;
    }

    /** Mock ChannelGateway.findByIds 用于 toProductBriefs 转换 */
    private void mockProductBriefs() {
        Channel product = new Channel();
        product.setId(PRODUCT_ID);
        product.setName("Test Product");
        when(channelGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));
    }
}