package com.codingas.gateway.application.tokenlimit;

import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitCreateRequest;
import com.codingas.gateway.adapter.admin.dto.tokenlimit.TokenLimitResponse;
import com.codingas.gateway.common.enums.ExceededAction;
import com.codingas.gateway.common.enums.PeriodType;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import com.codingas.gateway.domain.security.entity.TokenLimit;
import com.codingas.gateway.domain.security.entity.TokenLimit.LimitType;
import com.codingas.gateway.domain.security.entity.TokenLimit.TokenLimitStatus;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.TokenLimitGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TokenLimitApplication 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenLimitApplication 单元测试")
class TokenLimitApplicationTest {

    @Mock
    private TokenLimitGateway tokenLimitGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private TokenLimitApplication tokenLimitApplication;

    private User testUser;
    private Provider testProvider;
    private Model testModel;
    private TokenLimit testTokenLimit;
    private TokenLimitCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserCode("USER001");
        testUser.setUsername("testuser");

        // 初始化测试提供商
        testProvider = new Provider();
        testProvider.setId(1L);
        testProvider.setProviderCode("PROVIDER001");
        testProvider.setProviderName("OpenAI");

        // 初始化测试模型
        testModel = new Model();
        testModel.setId(1L);
        testModel.setModelCode("MODEL001");
        testModel.setDisplayName("GPT-4");

        // 初始化测试 Token 限额
        testTokenLimit = new TokenLimit();
        testTokenLimit.setId(1L);
        testTokenLimit.setLimitCode("LIMIT001");
        testTokenLimit.setUser(testUser);
        testTokenLimit.setProvider(testProvider);
        testTokenLimit.setModel(testModel);
        testTokenLimit.setLimitType(LimitType.USER_CUSTOM);
        testTokenLimit.setMaxTokens(new BigDecimal("10000"));
        testTokenLimit.setUsedTokens(BigDecimal.ZERO);
        testTokenLimit.setPeriodType(PeriodType.MONTHLY);
        testTokenLimit.setExceededAction(ExceededAction.REJECT);
        testTokenLimit.setStatus(TokenLimitStatus.ACTIVE);
        testTokenLimit.setCreatedAt(Instant.now());
        testTokenLimit.setUpdatedAt(Instant.now());

        // 初始化创建请求
        createRequest = new TokenLimitCreateRequest();
        createRequest.setLimitCode("LIMIT001");
        createRequest.setUserId(1L);
        createRequest.setProviderId(1L);
        createRequest.setModelId(1L);
        createRequest.setLimitType(LimitType.USER_CUSTOM);
        createRequest.setMaxTokens(new BigDecimal("10000"));
        createRequest.setPeriodType(PeriodType.MONTHLY);
        createRequest.setExceededAction(ExceededAction.REJECT);
    }

    @Nested
    @DisplayName("create 方法测试")
    class CreateMethodTests {

        @Test
        @DisplayName("创建 Token 限额成功")
        void create_success() {
            // given
            when(tokenLimitGateway.existsByLimitCode(createRequest.getLimitCode())).thenReturn(false);
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.of(testProvider));
            when(modelGateway.findById(createRequest.getModelId())).thenReturn(Optional.of(testModel));
            when(tokenLimitGateway.save(any(TokenLimit.class))).thenReturn(testTokenLimit);

            // when
            TokenLimitResponse response = tokenLimitApplication.create(createRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getLimitCode()).isEqualTo(testTokenLimit.getLimitCode());
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getProviderId()).isEqualTo(testProvider.getId());
            assertThat(response.getProviderName()).isEqualTo(testProvider.getProviderName());
            assertThat(response.getModelId()).isEqualTo(testModel.getId());
            assertThat(response.getModelName()).isEqualTo(testModel.getDisplayName());
            assertThat(response.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getRemainingTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getStatus()).isEqualTo(TokenLimitStatus.ACTIVE);

            verify(tokenLimitGateway).existsByLimitCode(createRequest.getLimitCode());
            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
            verify(modelGateway).findById(createRequest.getModelId());
            verify(tokenLimitGateway).save(any(TokenLimit.class));
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 限额代码重复")
        void create_duplicateLimitCode_throwsDuplicateResourceException() {
            // given
            when(tokenLimitGateway.existsByLimitCode(createRequest.getLimitCode())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> tokenLimitApplication.create(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("TokenLimit")
                .hasMessageContaining("limitCode");

            verify(tokenLimitGateway).existsByLimitCode(createRequest.getLimitCode());
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 用户不存在")
        void create_userNotFound_throwsResourceNotFoundException() {
            // given
            when(tokenLimitGateway.existsByLimitCode(createRequest.getLimitCode())).thenReturn(false);
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitApplication.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(String.valueOf(createRequest.getUserId()));

            verify(tokenLimitGateway).existsByLimitCode(createRequest.getLimitCode());
            verify(userGateway).findById(createRequest.getUserId());
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 提供商不存在")
        void create_providerNotFound_throwsResourceNotFoundException() {
            // given
            when(tokenLimitGateway.existsByLimitCode(createRequest.getLimitCode())).thenReturn(false);
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitApplication.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider")
                .hasMessageContaining(String.valueOf(createRequest.getProviderId()));

            verify(tokenLimitGateway).existsByLimitCode(createRequest.getLimitCode());
            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
        }

        @Test
        @DisplayName("创建 Token 限额失败 - 模型不存在")
        void create_modelNotFound_throwsResourceNotFoundException() {
            // given
            when(tokenLimitGateway.existsByLimitCode(createRequest.getLimitCode())).thenReturn(false);
            when(userGateway.findById(createRequest.getUserId())).thenReturn(Optional.of(testUser));
            when(providerGateway.findById(createRequest.getProviderId())).thenReturn(Optional.of(testProvider));
            when(modelGateway.findById(createRequest.getModelId())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitApplication.create(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Model")
                .hasMessageContaining(String.valueOf(createRequest.getModelId()));

            verify(tokenLimitGateway).existsByLimitCode(createRequest.getLimitCode());
            verify(userGateway).findById(createRequest.getUserId());
            verify(providerGateway).findById(createRequest.getProviderId());
            verify(modelGateway).findById(createRequest.getModelId());
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdMethodTests {

        @Test
        @DisplayName("根据 ID 获取 Token 限额成功")
        void getById_existingId_returnsTokenLimit() {
            // given
            when(tokenLimitGateway.findById(1L)).thenReturn(Optional.of(testTokenLimit));

            // when
            TokenLimitResponse response = tokenLimitApplication.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testTokenLimit.getId());
            assertThat(response.getLimitCode()).isEqualTo(testTokenLimit.getLimitCode());
            assertThat(response.getUserId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getProviderId()).isEqualTo(testProvider.getId());
            assertThat(response.getProviderName()).isEqualTo(testProvider.getProviderName());
            assertThat(response.getModelId()).isEqualTo(testModel.getId());
            assertThat(response.getModelName()).isEqualTo(testModel.getDisplayName());
            assertThat(response.getMaxTokens()).isEqualByComparingTo(testTokenLimit.getMaxTokens());
            assertThat(response.getUsedTokens()).isEqualByComparingTo(testTokenLimit.getUsedTokens());
            assertThat(response.getStatus()).isEqualTo(TokenLimitStatus.ACTIVE);

            verify(tokenLimitGateway).findById(1L);
        }

        @Test
        @DisplayName("根据 ID 获取 Token 限额失败 - 不存在")
        void getById_nonExistingId_throwsResourceNotFoundException() {
            // given
            when(tokenLimitGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> tokenLimitApplication.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("TokenLimit")
                .hasMessageContaining("99");

            verify(tokenLimitGateway).findById(99L);
        }
    }
}