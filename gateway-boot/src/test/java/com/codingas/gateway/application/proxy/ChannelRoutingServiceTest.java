package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ChannelRoutingService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelRoutingServiceTest {

    @Mock
    private ProductRoutingService productRoutingService;

    private ChannelRoutingService channelRoutingService;

    @BeforeEach
    void setUp() {
        channelRoutingService = new ChannelRoutingService(productRoutingService);
    }

    @Nested
    @DisplayName("resolve 方法测试")
    class ResolveTests {

        @Test
        @DisplayName("新架构认证结果走 ProductRoutingService")
        void resolve_newArchitecture_usesProductRouting() {
            Identity identity = Identity.of(1L, "USER", 101L);
            RoutingContext expected = new RoutingContext(
                200L, "https://api.openai.com", Protocol.OPENAI, "sk-test-key", RoutingStrategy.WEIGHTED);

            when(productRoutingService.resolve(eq(101L), eq("gpt-4o"), eq("openai"))).thenReturn(expected);

            RoutingContext ctx = channelRoutingService.resolve(identity, "gpt-4o", "openai");

            assertThat(ctx.channelId()).isEqualTo(200L);
            verify(productRoutingService).resolve(101L, "gpt-4o", "openai");
        }

        @Test
        @DisplayName("新架构认证结果使用 anthropic 协议")
        void resolve_newArchitecture_anthropicProtocol_usesAnthropicProtocol() {
            Identity identity = Identity.of(1L, "USER", 101L);
            RoutingContext expected = new RoutingContext(
                200L, "https://api.anthropic.com", Protocol.ANTHROPIC, "sk-ant-key", RoutingStrategy.WEIGHTED);

            when(productRoutingService.resolve(eq(101L), eq("claude-3-opus"), eq("anthropic"))).thenReturn(expected);

            RoutingContext ctx = channelRoutingService.resolve(identity, "claude-3-opus", "anthropic");

            assertThat(ctx.channelId()).isEqualTo(200L);
            verify(productRoutingService).resolve(101L, "claude-3-opus", "anthropic");
        }

        @Test
        @DisplayName("协议为 null 时传递给 ProductRoutingService")
        void resolve_nullProtocol_passesToProductRouting() {
            Identity identity = Identity.of(1L, "USER", 101L);
            RoutingContext expected = new RoutingContext(
                200L, "https://api.openai.com", Protocol.OPENAI, "sk-test-key", null);

            when(productRoutingService.resolve(eq(101L), eq("gpt-4o"), eq((String) null))).thenReturn(expected);

            RoutingContext ctx = channelRoutingService.resolve(identity, "gpt-4o", null);

            assertThat(ctx.channelId()).isEqualTo(200L);
            verify(productRoutingService).resolve(101L, "gpt-4o", null);
        }
    }
}
