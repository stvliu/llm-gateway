package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
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
    private ProductGateway productGateway;

    @InjectMocks
    private UserApiKeyServiceImpl service;

    private static final Long TEAM_ID = 1L;
    private static final Long USER_ID = 50L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建密钥成功")
        void create_success() {
            UserApiKey saved = createSampleApiKey();
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenReturn(saved);

            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    TEAM_ID, USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
            );
            UserApiKeyCreateResponse response = service.create(request);

            assertThat(response).isNotNull();
            assertThat(response.apiKeyPlain()).startsWith("sk-");
            assertThat(response.id()).isEqualTo(API_KEY_ID);
            verify(userApiKeyGateway).save(argThat(key ->
                    key.getUserId().equals(USER_ID) && key.getTeamId().equals(TEAM_ID)
            ));
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("查询用户的所有密钥")
        void findByUserId_success() {
            Product product = new Product();
            product.setId(PRODUCT_ID);
            product.setName("测试产品");
            when(productGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findByUserId(USER_ID)).thenReturn(List.of(apiKey));

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
    @DisplayName("listByTeamId 方法测试")
    class ListByTeamIdTests {

        @Test
        @DisplayName("查询团队下的所有密钥")
        void listByTeamId_success() {
            Product product = new Product();
            product.setId(PRODUCT_ID);
            product.setName("测试产品");
            when(productGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findByTeamId(TEAM_ID)).thenReturn(List.of(apiKey));

            List<UserApiKeyResponse> responses = service.listByTeamId(TEAM_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).teamId()).isEqualTo(TEAM_ID);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("查询存在的密钥")
        void getById_success() {
            Product product = new Product();
            product.setId(PRODUCT_ID);
            product.setName("测试产品");
            when(productGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

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
            Product product = new Product();
            product.setId(PRODUCT_ID);
            product.setName("测试产品");
            when(productGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));

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
            Product product = new Product();
            product.setId(PRODUCT_ID);
            product.setName("测试产品");
            when(productGateway.findByIds(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            UserApiKey apiKey = createSampleApiKey();
            when(userApiKeyGateway.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
            when(userApiKeyGateway.save(any(UserApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

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
        @DisplayName("删除密钥 — 直接调用 deleteById")
        void delete_success() {
            service.delete(API_KEY_ID);

            verify(userApiKeyGateway).deleteById(API_KEY_ID);
        }
    }

    private UserApiKey createSampleApiKey() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setId(API_KEY_ID);
        apiKey.setTeamId(TEAM_ID);
        apiKey.setUserId(USER_ID);
        apiKey.setProductIds(List.of(PRODUCT_ID));
        apiKey.setKeyPlain("sk-abc1xxxxx");
        apiKey.setKeyPrefix("sk-abc1");
        apiKey.setName("test-key");
        apiKey.setModels(List.of("gpt-4o"));
        apiKey.setQuotaLimit(100000L);
        apiKey.setState(UserApiKeyState.ACTIVE);
        return apiKey;
    }
}
