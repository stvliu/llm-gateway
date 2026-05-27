package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
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
import static org.mockito.Mockito.when;

/**
 * CredentialResolver 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialResolver 单元测试")
class CredentialResolverTest {

    @Mock
    private ChannelCredentialGateway channelCredentialGateway;

    @InjectMocks
    private CredentialResolver credentialResolver;

    @Nested
    @DisplayName("resolve 凭证解析")
    class ResolveTests {

        @Test
        @DisplayName("优先使用默认凭证解析成功")
        void resolve_defaultCredential_returnsApiKey() {
            // given
            ChannelCredential defaultKey = new ChannelCredential();
            defaultKey.setId(1L);
            defaultKey.setChannelId(10L);
            defaultKey.setApiKeyPlain("sk-default-key");
            defaultKey.setState(CredentialState.ACTIVE);

            when(channelCredentialGateway.findDefaultByChannelId(10L))
                    .thenReturn(Optional.of(defaultKey));

            // when
            String result = credentialResolver.resolve(10L);

            // then
            assertThat(result).isEqualTo("sk-default-key");
        }

        @Test
        @DisplayName("默认凭证不可用时回退到活跃凭证")
        void resolve_defaultUnavailable_fallsBackToActive() {
            // given
            ChannelCredential defaultKey = new ChannelCredential();
            defaultKey.setId(1L);
            defaultKey.setChannelId(10L);
            defaultKey.setApiKeyPlain("sk-default-key");
            defaultKey.setState(CredentialState.INACTIVE);

            ChannelCredential activeKey = new ChannelCredential();
            activeKey.setId(2L);
            activeKey.setChannelId(10L);
            activeKey.setApiKeyPlain("sk-active-key");
            activeKey.setState(CredentialState.ACTIVE);

            when(channelCredentialGateway.findDefaultByChannelId(10L))
                    .thenReturn(Optional.of(defaultKey));
            when(channelCredentialGateway.findActiveByChannelId(10L))
                    .thenReturn(List.of(activeKey));

            // when
            String result = credentialResolver.resolve(10L);

            // then
            assertThat(result).isEqualTo("sk-active-key");
        }

        @Test
        @DisplayName("无默认凭证时使用活跃凭证")
        void resolve_noDefault_usesActiveCredential() {
            // given
            ChannelCredential activeKey = new ChannelCredential();
            activeKey.setId(2L);
            activeKey.setChannelId(10L);
            activeKey.setApiKeyPlain("sk-active-key");
            activeKey.setState(CredentialState.ACTIVE);

            when(channelCredentialGateway.findDefaultByChannelId(10L))
                    .thenReturn(Optional.empty());
            when(channelCredentialGateway.findActiveByChannelId(10L))
                    .thenReturn(List.of(activeKey));

            // when
            String result = credentialResolver.resolve(10L);

            // then
            assertThat(result).isEqualTo("sk-active-key");
        }

        @Test
        @DisplayName("无可用凭证时抛出 ResourceNotFoundException")
        void resolve_noCredential_throwsException() {
            // given
            when(channelCredentialGateway.findDefaultByChannelId(10L))
                    .thenReturn(Optional.empty());
            when(channelCredentialGateway.findActiveByChannelId(10L))
                    .thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> credentialResolver.resolve(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelCredential")
                    .hasMessageContaining("10");
        }
    }
}
