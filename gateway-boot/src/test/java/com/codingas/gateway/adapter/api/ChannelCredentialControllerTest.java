package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channelcredential.ChannelCredentialService;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
import com.codingas.gateway.domain.supply.enums.CredentialState;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private ChannelCredentialResponse createResponse(Long id, CredentialState state) {
        return new ChannelCredentialResponse(
                id, CHANNEL_ID, "sk-test-", "sk-tes****est-", "test-key", "test key",
                1, 1, state, Instant.now(), Instant.now()
        );
    }

    @Test
    void list_success() {
        when(channelCredentialService.listByChannelId(CHANNEL_ID))
                .thenReturn(List.of(createResponse(1L, CredentialState.ACTIVE)));

        List<ChannelCredentialResponse> result = controller.list(CHANNEL_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void get_success() {
        ChannelCredentialDetailResponse detailResponse = new ChannelCredentialDetailResponse(
                1L, CHANNEL_ID, "sk-test-", "sk-test-api-key-12345",
                "test-key", "test key",
                1, 1, CredentialState.ACTIVE, Instant.now(), Instant.now()
        );
        when(channelCredentialService.getDetailById(CHANNEL_ID, 1L)).thenReturn(detailResponse);

        ChannelCredentialDetailResponse result = controller.get(CHANNEL_ID, 1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.apiKeyPlain()).isEqualTo("sk-test-api-key-12345");
    }

    @Test
    void create_success() {
        ChannelCredentialCreateRequest request = new ChannelCredentialCreateRequest(
                CHANNEL_ID, "sk-test-key", 1, 1, "test key"
        );
        ChannelCredentialCreateResponse createResponse = new ChannelCredentialCreateResponse(
                1L, "sk-test-", "sk-test-key"
        );
        when(channelCredentialService.create(eq(CHANNEL_ID), any(ChannelCredentialCreateRequest.class))).thenReturn(createResponse);

        ChannelCredentialCreateResponse result = controller.create(CHANNEL_ID, request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.apiKeyPlain()).isEqualTo("sk-test-key");
    }

    @Test
    void update_success() {
        ChannelCredentialUpdateRequest request = new ChannelCredentialUpdateRequest(
                10, 5, null, null, null
        );
        when(channelCredentialService.update(eq(CHANNEL_ID), eq(1L), any(ChannelCredentialUpdateRequest.class)))
                .thenReturn(createResponse(1L, CredentialState.ACTIVE));

        ChannelCredentialResponse result = controller.update(CHANNEL_ID, 1L, request);

        assertThat(result).isNotNull();
    }

    @Test
    void delete_success() {
        controller.delete(CHANNEL_ID, 1L);
        verify(channelCredentialService).delete(CHANNEL_ID, 1L);
    }
}
