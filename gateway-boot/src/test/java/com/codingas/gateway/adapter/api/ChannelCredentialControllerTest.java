package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channelcredential.ChannelCredentialService;
import com.codingas.gateway.application.channelcredential.dto.*;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private ChannelCredentialResponse buildResponse() {
        return new ChannelCredentialResponse(
                CREDENTIAL_ID, CHANNEL_ID, "sk-test-", "sk-tes****est-", "test-key", "test key",
                1, 1, CredentialState.ACTIVE, Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("查询凭证列表")
    class ListTest {

        @Test
        @DisplayName("返回渠道下的凭证列表")
        void returnsCredentialList() {
            when(channelCredentialService.listByChannelId(CHANNEL_ID))
                    .thenReturn(List.of(buildResponse()));

            List<ChannelCredentialResponse> result = controller.list(CHANNEL_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(CREDENTIAL_ID);
            verify(channelCredentialService).listByChannelId(CHANNEL_ID);
        }
    }

    @Nested
    @DisplayName("查询凭证详情")
    class GetTest {

        @Test
        @DisplayName("返回含明文的凭证详情")
        void returnsDetail() {
            var detail = new ChannelCredentialDetailResponse(
                    CREDENTIAL_ID, CHANNEL_ID, "sk-test-", "sk-test-api-key-12345",
                    "test-key", "test key",
                    1, 1, CredentialState.ACTIVE, Instant.now(), Instant.now()
            );
            when(channelCredentialService.getDetailById(CHANNEL_ID, CREDENTIAL_ID)).thenReturn(detail);

            ChannelCredentialDetailResponse result = controller.get(CHANNEL_ID, CREDENTIAL_ID);

            assertThat(result.id()).isEqualTo(CREDENTIAL_ID);
            assertThat(result.apiKeyPlain()).isEqualTo("sk-test-api-key-12345");
        }
    }

    @Nested
    @DisplayName("创建凭证")
    class CreateTest {

        @Test
        @DisplayName("创建成功返回含明文的响应")
        void createsCredential() {
            var request = new ChannelCredentialCreateRequest(CHANNEL_ID, "sk-test-key", 1, 1, "test key");
            var createResponse = new ChannelCredentialCreateResponse(CREDENTIAL_ID, "sk-test-key");
            when(channelCredentialService.create(eq(CHANNEL_ID), any(ChannelCredentialCreateRequest.class)))
                    .thenReturn(createResponse);

            ChannelCredentialCreateResponse result = controller.create(CHANNEL_ID, request);

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
            var request = new ChannelCredentialUpdateRequest(10, 5, null, null, null);
            when(channelCredentialService.update(eq(CHANNEL_ID), eq(CREDENTIAL_ID), any(ChannelCredentialUpdateRequest.class)))
                    .thenReturn(buildResponse());

            ChannelCredentialResponse result = controller.update(CHANNEL_ID, CREDENTIAL_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CREDENTIAL_ID);
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