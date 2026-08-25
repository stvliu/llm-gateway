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
package com.codingas.gateway.web.api;

import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialCreateCommand;
import com.codingas.gateway.provider.channel.ChannelCredentialService;
import com.codingas.gateway.provider.channel.ChannelCredentialUpdateCommand;
import com.codingas.gateway.web.api.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChannelCredentialController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ChannelCredentialControllerTest {

    @Mock
    private ChannelCredentialService channelCredentialService;

    @InjectMocks
    private ChannelCredentialController controller;

    private static final Long CHANNEL_ID = 100L;
    private static final Long CREDENTIAL_ID = 1L;

    private ChannelCredential buildCredential() {
        ChannelCredential credential = new ChannelCredential();
        credential.setId(CREDENTIAL_ID);
        credential.setChannelId(CHANNEL_ID);
        credential.setApiKeyPrefix("sk-test-");
        credential.setApiKeyPlain("sk-tes****est-");
        credential.setName("test-key");
        credential.setWeight(1);
        credential.setPriority(1);
        credential.setCreatedAt(Instant.now());
        credential.setUpdatedAt(Instant.now());
        return credential;
    }

    @Nested
    @DisplayName("查询凭证列表")
    class ListTest {

        @Test
        @DisplayName("返回渠道下的凭证列表")
        void returnsCredentialList() {
            when(channelCredentialService.listByChannelId(CHANNEL_ID))
                    .thenReturn(List.of(buildCredential()));

            List<ChannelCredentialResponse> result = controller.list(CHANNEL_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(CREDENTIAL_ID);
            verify(channelCredentialService).listByChannelId(CHANNEL_ID);
        }
    }

    @Nested
    @DisplayName("查询凭证详情")
    class GetTest {

        @Test
        @DisplayName("返回含明文的凭证详情")
        void returnsDetail() {
            ChannelCredential credential = buildCredential();
            credential.setApiKeyPlain("sk-test-api-key-12345");
            when(channelCredentialService.getDetailById(CHANNEL_ID, CREDENTIAL_ID)).thenReturn(credential);

            ChannelCredentialResponse result = controller.get(CHANNEL_ID, CREDENTIAL_ID);

            assertThat(result.getId()).isEqualTo(CREDENTIAL_ID);
            assertThat(result.getApiKeyPlain()).isEqualTo("sk-test-api-key-12345");
        }
    }

    @Nested
    @DisplayName("创建凭证")
    class CreateTest {

        @Test
        @DisplayName("创建成功返回含明文的响应")
        void createsCredential() {
            // 请求体不含 channelId，由适配层补全
            var request = new ChannelCredentialCreateRequest("sk-test-key", 1, 1, "test key");
            var saved = buildCredential();
            saved.setApiKeyPlain("sk-test-key");
            when(channelCredentialService.create(any(ChannelCredentialCreateCommand.class)))
                    .thenReturn(saved);

            ChannelCredentialCreateResponse result = controller.create(CHANNEL_ID, request);

            // 验证适配层补全了 channelId
            var captured = ArgumentCaptor.forClass(ChannelCredentialCreateCommand.class);
            verify(channelCredentialService).create(captured.capture());
            assertThat(captured.getValue().channelId()).isEqualTo(CHANNEL_ID);

            assertThat(result.id()).isEqualTo(CREDENTIAL_ID);
            assertThat(result.apiKeyPlain()).isEqualTo("sk-test-key");
        }
    }

    @Nested
    @DisplayName("更新凭证")
    class UpdateTest {

        @Test
        @DisplayName("更新成功返回更新后的凭证")
        void updatesCredential() {
            // 请求体不含 channelId 和 id，由适配层补全
            var request = new ChannelCredentialUpdateRequest(10, 5, null, null);
            when(channelCredentialService.update(any(ChannelCredentialUpdateCommand.class)))
                    .thenReturn(buildCredential());

            ChannelCredentialResponse result = controller.update(CHANNEL_ID, CREDENTIAL_ID, request);

            // 验证适配层补全了 channelId 和 id
            var captured = ArgumentCaptor.forClass(ChannelCredentialUpdateCommand.class);
            verify(channelCredentialService).update(captured.capture());
            assertThat(captured.getValue().channelId()).isEqualTo(CHANNEL_ID);
            assertThat(captured.getValue().id()).isEqualTo(CREDENTIAL_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CREDENTIAL_ID);
        }
    }

    @Nested
    @DisplayName("删除凭证")
    class DeleteTest {

        @Test
        @DisplayName("调用删除服务方法")
        void deletesCredential() {
            controller.delete(CHANNEL_ID, CREDENTIAL_ID);
            verify(channelCredentialService).delete(CHANNEL_ID, CREDENTIAL_ID);
        }
    }
}
