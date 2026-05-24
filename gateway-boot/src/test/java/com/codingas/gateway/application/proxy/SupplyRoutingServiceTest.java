package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.service.ChannelDomainService;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.service.UserApiKeyDomainService;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SupplyRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SupplyRoutingService 测试")
class SupplyRoutingServiceTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ChannelCredentialGateway channelCredentialGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ChannelDomainService channelDomainService;

    private UserApiKeyDomainService userApiKeyDomainService;
    private SupplyRoutingService service;

    @BeforeEach
    void setUp() {
        userApiKeyDomainService = new UserApiKeyDomainService();
        service = new SupplyRoutingService(
                userApiKeyGateway, channelGateway, channelCredentialGateway, providerGateway,
                userApiKeyDomainService, channelDomainService);
    }

    private Channel createChannel(Long id, Long providerId, String name, ChannelState state) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(providerId);
        channel.setName(name);
        channel.setState(state);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setTimeout(60);
        return channel;
    }

    private ChannelEndpoint createEndpoint(Long id, Long channelId, Protocol protocol, String url) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setId(id);
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(url);
        endpoint.setState(ChannelEndpointState.ACTIVE);
        return endpoint;
    }

    private UserApiKey createUserApiKey(Long id, List<Long> channelIds, List<String> models) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setId(id);
        userApiKey.setChannelIds(channelIds);
        userApiKey.setState(UserApiKeyState.ACTIVE);
        userApiKey.setModels(models);
        return userApiKey;
    }

    private ChannelCredential createCredential(Long id, Long channelId, Integer priority, Integer weight, CredentialState state) {
        ChannelCredential apiKey = new ChannelCredential();
        apiKey.setId(id);
        apiKey.setChannelId(channelId);
        apiKey.setPriority(priority);
        apiKey.setWeight(weight);
        apiKey.setState(state);
        apiKey.setApiKeyPlain("sk-provider-key-" + id);
        return apiKey;
    }

    private Provider createProvider(Long id, String name) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setName(name);
        return provider;
    }

    // ==================== resolve(Identity) 测试 ====================

    @Nested
    @DisplayName("resolve(Identity) 路由测试")
    class IdentityResolveTests {

        @Test
        @DisplayName("基于 Identity 解析路由上下文")
        void resolve_withIdentity_delegatesToCredentialId() {
            Identity identity = Identity.of(1L, "USER", 101L);
            Channel channel = createChannel(200L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(101L, List.of(200L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 200L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");
            ChannelEndpoint endpoint = createEndpoint(300L, 200L, Protocol.OPENAI, "https://api.openai.com");

            when(userApiKeyGateway.findById(101L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(200L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(200L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
                    .thenReturn(endpoint);

            RoutingContext ctx = service.resolve(identity, "gpt-4o", "openai");

            assertThat(ctx.channelId()).isEqualTo(200L);
            assertThat(ctx.channelEndpointId()).isEqualTo(300L);
            assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(ctx.needsProtocolAdaptation()).isFalse();
            assertThat(ctx.timeout()).isEqualTo(60);
        }
    }

    // ==================== resolve(userApiKeyId) 测试 ====================

    @Nested
    @DisplayName("resolve(userApiKeyId) 路由测试")
    class UserApiKeyIdResolveTests {

        @Test
        @DisplayName("成功解析路由 — 使用默认 ChannelCredential")
        void resolve_success_withDefaultApiKey() {
            Channel channel = createChannel(100L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");
            ChannelEndpoint endpoint = createEndpoint(200L, 100L, Protocol.OPENAI, "https://api.openai.com");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
                    .thenReturn(endpoint);

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx).isNotNull();
            assertThat(ctx.channelId()).isEqualTo(100L);
            assertThat(ctx.channelEndpointId()).isEqualTo(200L);
            assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(ctx.endpointUrl()).isEqualTo("https://api.openai.com");
            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-1");
            assertThat(ctx.timeout()).isEqualTo(60);
            assertThat(ctx.needsProtocolAdaptation()).isFalse();
        }

        @Test
        @DisplayName("成功解析路由 — 跨协议降级")
        void resolve_success_crossProtocol() {
            Channel channel = createChannel(100L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("claude-3-opus"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");
            // 渠道只有 ANTHROPIC 端点，入站是 OPENAI，需要跨协议转换
            ChannelEndpoint endpoint = createEndpoint(200L, 100L, Protocol.ANTHROPIC, "https://api.anthropic.com");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
                    .thenReturn(endpoint);

            RoutingContext ctx = service.resolve(1L, "claude-3-opus", "openai");

            assertThat(ctx).isNotNull();
            assertThat(ctx.upstreamProtocol()).isEqualTo(Protocol.ANTHROPIC);
            assertThat(ctx.needsProtocolAdaptation()).isTrue();
        }

        @Test
        @DisplayName("失败 — UserApiKey 不存在")
        void resolve_userApiKeyNotFound_throwsException() {
            when(userApiKeyGateway.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(999L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("UserApiKey");
        }

        @Test
        @DisplayName("失败 — 无可用的 ChannelCredential")
        void resolve_noAvailableApiKey_throwsException() {
            Channel channel = createChannel(100L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.empty());
            when(channelCredentialGateway.findActiveByChannelId(100L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.resolve(1L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("ChannelCredential");
        }
    }

    // ==================== Credential 选择策略测试 ====================

    @Nested
    @DisplayName("ChannelCredential 选择策略测试")
    class ApiKeySelectionTests {

        @Test
        @DisplayName("默认 Key 可用时优先使用")
        void selectDefaultKey_whenAvailable() {
            Channel channel = createChannel(100L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");
            ChannelEndpoint endpoint = createEndpoint(200L, 100L, Protocol.OPENAI, "https://api.openai.com");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
                    .thenReturn(endpoint);

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-1");
            verify(channelCredentialGateway, never()).findActiveByChannelId(anyLong());
        }

        @Test
        @DisplayName("默认 Key 不可用时降级到活跃 Key 列表")
        void fallbackToActiveKeys_whenDefaultNotAvailable() {
            Channel channel = createChannel(100L, 1L, "Test Channel", ChannelState.ACTIVE);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.DISABLED);
            ChannelCredential activeKey = createCredential(2L, 100L, 2, 50, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");
            ChannelEndpoint endpoint = createEndpoint(200L, 100L, Protocol.OPENAI, "https://api.openai.com");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(channelCredentialGateway.findActiveByChannelId(100L)).thenReturn(List.of(activeKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));
            when(channelDomainService.resolveEndpoint(any(Channel.class), eq(Protocol.OPENAI)))
                    .thenReturn(endpoint);

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-2");
        }
    }
}