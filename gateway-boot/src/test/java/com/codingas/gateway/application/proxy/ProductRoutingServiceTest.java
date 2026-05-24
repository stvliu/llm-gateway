package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * ProductRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRoutingService 测试")
class ProductRoutingServiceTest {

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
    private ProductRoutingService service;

    @BeforeEach
    void setUp() {
        userApiKeyDomainService = new UserApiKeyDomainService();
        service = new ProductRoutingService(
                userApiKeyGateway, channelGateway, channelCredentialGateway, providerGateway,
                userApiKeyDomainService, channelDomainService);
    }

    private Channel createChannel(Long id, Long providerId, String name, ChannelState state, Protocol protocol) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setProviderId(providerId);
        channel.setProviderName("TestProvider");
        channel.setName(name);
        channel.setState(state);
        channel.setBillingMode(BillingMode.PAY_AS_YOU_GO);
        channel.setProtocol(protocol);
        channel.setEndpointUrl(protocol == Protocol.ANTHROPIC
                ? "https://api.anthropic.com"
                : "https://api.openai.com");
        return channel;
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

    @Nested
    @DisplayName("resolve 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("成功解析路由 — 使用默认 ChannelCredential")
        void resolve_success_withDefaultApiKey() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.OPENAI);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx).isNotNull();
            assertThat(ctx.channelId()).isEqualTo(100L);
            assertThat(ctx.protocol()).isEqualTo(Protocol.OPENAI);
            assertThat(ctx.endpoint()).isEqualTo("https://api.openai.com");
            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-1");
        }

        @Test
        @DisplayName("成功解析路由 — 使用 anthropic 协议")
        void resolve_success_withAnthropicProtocol() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.ANTHROPIC);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("claude-3-opus"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "claude-3-opus", "anthropic");

            assertThat(ctx).isNotNull();
            assertThat(ctx.protocol()).isEqualTo(Protocol.ANTHROPIC);
            assertThat(ctx.endpoint()).isEqualTo("https://api.anthropic.com");
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
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.OPENAI);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.empty());
            when(channelCredentialGateway.findActiveByChannelId(100L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.resolve(1L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("ChannelCredential");
        }

        @Test
        @DisplayName("失败 — Provider 不存在")
        void resolve_providerNotFound_throwsException() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.OPENAI);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(1L, "gpt-4o", "openai"))
                .isInstanceOf(com.codingas.gateway.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Provider");
        }
    }

    @Nested
    @DisplayName("ChannelCredential 选择策略测试")
    class ApiKeySelectionTests {

        @Test
        @DisplayName("默认 Key 可用时优先使用")
        void selectDefaultKey_whenAvailable() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.OPENAI);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-1");
            verify(channelCredentialGateway, never()).findActiveByChannelId(anyLong());
        }

        @Test
        @DisplayName("默认 Key 不可用时降级到活跃 Key 列表")
        void fallbackToActiveKeys_whenDefaultNotAvailable() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.OPENAI);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("gpt-4o"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.DISABLED);
            ChannelCredential activeKey = createCredential(2L, 100L, 2, 50, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "OpenAI");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(channelCredentialGateway.findActiveByChannelId(100L)).thenReturn(List.of(activeKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "gpt-4o", "openai");

            assertThat(ctx.providerApiKey()).isEqualTo("sk-provider-key-2");
        }
    }

    @Nested
    @DisplayName("端点解析测试")
    class EndpointResolutionTests {

        @Test
        @DisplayName("使用渠道配置的端点和协议")
        void useChannelEndpointAndProtocol() {
            Channel channel = createChannel(100L, 1L, "Test Product", ChannelState.ACTIVE, Protocol.ANTHROPIC);
            UserApiKey userApiKey = createUserApiKey(1L, List.of(100L), List.of("claude-3-opus"));
            ChannelCredential defaultKey = createCredential(1L, 100L, 1, 100, CredentialState.ACTIVE);
            Provider provider = createProvider(1L, "Anthropic");

            when(userApiKeyGateway.findById(1L)).thenReturn(Optional.of(userApiKey));
            when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel));
            when(channelCredentialGateway.findDefaultByChannelId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerGateway.findById(1L)).thenReturn(Optional.of(provider));

            RoutingContext ctx = service.resolve(1L, "claude-3-opus", "anthropic");

            assertThat(ctx.endpoint()).isEqualTo("https://api.anthropic.com");
            assertThat(ctx.protocol()).isEqualTo(Protocol.ANTHROPIC);
        }
    }
}
