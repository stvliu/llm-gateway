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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
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

            when(channelCredentialGateway.findDefaultByChannelId(10L))
                    .thenReturn(Optional.of(defaultKey));

            // when
            String result = credentialResolver.resolve(10L);

            // then
            assertThat(result).isEqualTo("sk-default-key");
        }

        @Test
        @DisplayName("无默认凭证时使用活跃凭证")
        void resolve_noDefault_usesActiveCredential() {
            // given
            ChannelCredential activeKey = new ChannelCredential();
            activeKey.setId(2L);
            activeKey.setChannelId(10L);
            activeKey.setApiKeyPlain("sk-active-key");

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
