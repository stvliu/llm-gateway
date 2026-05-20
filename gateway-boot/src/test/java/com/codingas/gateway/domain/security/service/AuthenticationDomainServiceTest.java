package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.enums.ProductState;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.enums.UserState;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.TeamState;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AuthenticationDomainService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationDomainService 测试")
class AuthenticationDomainServiceTest {

    @Mock
    private ApiKeyGateway apiKeyGateway;

    @Mock
    private UserGateway userGateway;

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private TeamGateway teamGateway;

    @Mock
    private ProductGateway productGateway;

    @Mock
    private ApiKeyEncryptionDomainService encryptionService;

    private AuthenticationDomainService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationDomainService(apiKeyGateway, userGateway, userApiKeyGateway, teamGateway, productGateway, encryptionService);
    }

    @Nested
    @DisplayName("authenticate 方法测试")
    class AuthenticateTests {

        @Test
        @DisplayName("新架构认证成功 — UserApiKey、Team、Product 都活跃")
        void authenticate_newArch_success() {
            // given
            String apiKey = "sk-new-arch-key";
            String keyHash = "hashed-key";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setProductId(200L);
            userApiKey.setTeamId(300L);
            userApiKey.setState(UserApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            Team team = new Team();
            team.setId(300L);
            team.setState(TeamState.ACTIVE);

            Product product = new Product();
            product.setId(200L);
            product.setState(ProductState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(userApiKey));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(teamGateway.findById(300L)).thenReturn(Optional.of(team));
            when(productGateway.findById(200L)).thenReturn(Optional.of(product));

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.newArchitecture()).isTrue();
            assertThat(result.userId()).isEqualTo(1L);
            assertThat(result.role()).isEqualTo("USER");
            assertThat(result.userApiKeyId()).isEqualTo(101L);
            assertThat(result.productId()).isEqualTo(200L);
            assertThat(result.teamId()).isEqualTo(300L);
        }

        @Test
        @DisplayName("新架构认证失败 — Team 不存在")
        void authenticate_newArch_teamNotFound_returnsNull() {
            // given
            String apiKey = "sk-no-team-key";
            String keyHash = "hashed-no-team";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setProductId(200L);
            userApiKey.setTeamId(300L);
            userApiKey.setState(UserApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(userApiKey));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(teamGateway.findById(300L)).thenReturn(Optional.empty());

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("新架构认证失败 — Team 已禁用")
        void authenticate_newArch_teamInactive_returnsNull() {
            // given
            String apiKey = "sk-inactive-team-key";
            String keyHash = "hashed-inactive-team";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setProductId(200L);
            userApiKey.setTeamId(300L);
            userApiKey.setState(UserApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            Team team = new Team();
            team.setId(300L);
            team.setState(TeamState.INACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(userApiKey));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(teamGateway.findById(300L)).thenReturn(Optional.of(team));

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("新架构认证失败 — Product 不存在")
        void authenticate_newArch_productNotFound_returnsNull() {
            // given
            String apiKey = "sk-no-product-key";
            String keyHash = "hashed-no-product";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setProductId(200L);
            userApiKey.setTeamId(300L);
            userApiKey.setState(UserApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            Team team = new Team();
            team.setId(300L);
            team.setState(TeamState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(userApiKey));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(teamGateway.findById(300L)).thenReturn(Optional.of(team));
            when(productGateway.findById(200L)).thenReturn(Optional.empty());

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("新架构认证失败 — Product 已禁用")
        void authenticate_newArch_productInactive_returnsNull() {
            // given
            String apiKey = "sk-inactive-product-key";
            String keyHash = "hashed-inactive-product";

            UserApiKey userApiKey = new UserApiKey();
            userApiKey.setId(101L);
            userApiKey.setUserId(1L);
            userApiKey.setProductId(200L);
            userApiKey.setTeamId(300L);
            userApiKey.setState(UserApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            Team team = new Team();
            team.setId(300L);
            team.setState(TeamState.ACTIVE);

            Product product = new Product();
            product.setId(200L);
            product.setState(ProductState.INACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(userApiKey));
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(teamGateway.findById(300L)).thenReturn(Optional.of(team));
            when(productGateway.findById(200L)).thenReturn(Optional.of(product));

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("旧架构降级认证成功 — UserApiKey 不存在，GatewayApiKey 存在")
        void authenticate_legacy_fallback_success() {
            // given
            String apiKey = "sk-legacy-key";
            String keyHash = "hashed-legacy-key";

            GatewayApiKey gatewayApiKey = new GatewayApiKey();
            gatewayApiKey.setId(10L);
            gatewayApiKey.setUserId(2L);
            gatewayApiKey.setState(GatewayApiKeyState.ACTIVE);

            User user = new User();
            user.setId(2L);
            user.setRole("ADMIN");
            user.setState(UserState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.empty());
            when(apiKeyGateway.findByKeyHash(keyHash)).thenReturn(gatewayApiKey);
            when(userGateway.findById(2L)).thenReturn(Optional.of(user));

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.newArchitecture()).isFalse();
            assertThat(result.userId()).isEqualTo(2L);
            assertThat(result.role()).isEqualTo("ADMIN");
            assertThat(result.apiKeyId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("认证失败 — 两个路径都找不到 Key")
        void authenticate_notFound_returnsNull() {
            // given
            String apiKey = "sk-unknown-key";
            String keyHash = "hashed-unknown";

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.empty());
            when(apiKeyGateway.findByKeyHash(keyHash)).thenReturn(null);

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("认证失败 — UserApiKey 已禁用，降级到旧架构")
        void authenticate_userApiKeyInactive_fallsBackToLegacy() {
            // given
            String apiKey = "sk-disabled-new-key";
            String keyHash = "hashed-disabled";

            UserApiKey inactiveKey = new UserApiKey();
            inactiveKey.setId(101L);
            inactiveKey.setUserId(1L);
            inactiveKey.setState(UserApiKeyState.INACTIVE);

            GatewayApiKey legacyKey = new GatewayApiKey();
            legacyKey.setId(10L);
            legacyKey.setUserId(1L);
            legacyKey.setState(GatewayApiKeyState.ACTIVE);

            User user = new User();
            user.setId(1L);
            user.setRole("USER");
            user.setState(UserState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.of(inactiveKey));
            when(apiKeyGateway.findByKeyHash(keyHash)).thenReturn(legacyKey);
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.newArchitecture()).isFalse();
        }

        @Test
        @DisplayName("认证失败 — 用户不存在")
        void authenticate_userNotFound_returnsNull() {
            // given
            String apiKey = "sk-no-user-key";
            String keyHash = "hashed-no-user";

            GatewayApiKey gatewayApiKey = new GatewayApiKey();
            gatewayApiKey.setId(10L);
            gatewayApiKey.setUserId(999L);
            gatewayApiKey.setState(GatewayApiKeyState.ACTIVE);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.empty());
            when(apiKeyGateway.findByKeyHash(keyHash)).thenReturn(gatewayApiKey);
            when(userGateway.findById(999L)).thenReturn(Optional.empty());

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("认证失败 — GatewayApiKey 已禁用")
        void authenticate_gatewayApiKeyDisabled_returnsNull() {
            // given
            String apiKey = "sk-disabled-legacy-key";
            String keyHash = "hashed-disabled-legacy";

            GatewayApiKey disabledKey = new GatewayApiKey();
            disabledKey.setId(10L);
            disabledKey.setUserId(1L);
            disabledKey.setState(GatewayApiKeyState.DISABLED);

            when(encryptionService.hashKey(apiKey)).thenReturn(keyHash);
            when(userApiKeyGateway.findByKeyHash(keyHash)).thenReturn(Optional.empty());
            when(apiKeyGateway.findByKeyHash(keyHash)).thenReturn(disabledKey);

            // when
            UserAuthResult result = service.authenticate(apiKey);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空 API Key 返回 null")
        void authenticate_blankKey_returnsNull() {
            // when
            UserAuthResult result = service.authenticate("");

            // then
            assertThat(result).isNull();
            verifyNoInteractions(encryptionService, apiKeyGateway, userApiKeyGateway, teamGateway, productGateway);
        }

        @Test
        @DisplayName("null API Key 返回 null")
        void authenticate_nullKey_returnsNull() {
            // when
            UserAuthResult result = service.authenticate(null);

            // then
            assertThat(result).isNull();
            verifyNoInteractions(encryptionService, apiKeyGateway, userApiKeyGateway, teamGateway, productGateway);
        }
    }
}