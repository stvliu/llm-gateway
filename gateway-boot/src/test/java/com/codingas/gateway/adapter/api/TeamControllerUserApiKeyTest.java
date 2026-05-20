package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
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
 * TeamController 中 UserApiKey 端点单元测试
 */
@ExtendWith(MockitoExtension.class)
class TeamControllerUserApiKeyTest {

    @Mock
    private TeamService teamService;

    @Mock
    private UserApiKeyService userApiKeyService;

    @InjectMocks
    private TeamController controller;

    private static final Long TEAM_ID = 1L;
    private static final Long API_KEY_ID = 100L;

    private UserApiKeyResponse createResponse(Long id, UserApiKeyState state) {
        return new UserApiKeyResponse(
                id, TEAM_ID, 10L, "sk-abc1", "test-key",
                List.of("gpt-4o"), 100000L, state,
                Instant.now(), Instant.now()
        );
    }

    @Test
    void listApiKeys_success() {
        when(userApiKeyService.listByTeamId(TEAM_ID))
                .thenReturn(List.of(createResponse(API_KEY_ID, UserApiKeyState.ACTIVE)));

        List<UserApiKeyResponse> result = controller.listApiKeys(TEAM_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(API_KEY_ID);
    }

    @Test
    void getApiKey_success() {
        UserApiKeyDetailResponse detailResponse = new UserApiKeyDetailResponse(
                API_KEY_ID, TEAM_ID, 10L, "sk-abc1", "sk-abc1xxxxx", "test-key",
                List.of("gpt-4o"), 100000L, UserApiKeyState.ACTIVE,
                Instant.now(), Instant.now()
        );
        when(userApiKeyService.getDetailById(API_KEY_ID)).thenReturn(detailResponse);

        UserApiKeyDetailResponse result = controller.getApiKey(TEAM_ID, API_KEY_ID);

        assertThat(result.id()).isEqualTo(API_KEY_ID);
        assertThat(result.keyPlain()).isEqualTo("sk-abc1xxxxx");
    }

    @Test
    void createApiKey_success() {
        UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                TEAM_ID, 10L, "test-key", List.of("gpt-4o"), 100000L
        );
        UserApiKeyCreateResponse createResponse = new UserApiKeyCreateResponse(
                API_KEY_ID, "sk-abc1", "sk-abc1xxxxx"
        );
        when(userApiKeyService.create(any(UserApiKeyCreateRequest.class))).thenReturn(createResponse);

        var result = controller.createApiKey(TEAM_ID, request);

        assertThat(result.getStatusCode().value()).isEqualTo(201);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(API_KEY_ID);
    }

    @Test
    void updateApiKey_success() {
        UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                "updated-name", List.of("claude-3-5-sonnet"), null, null
        );
        when(userApiKeyService.update(eq(API_KEY_ID), any(UserApiKeyUpdateRequest.class)))
                .thenReturn(createResponse(API_KEY_ID, UserApiKeyState.ACTIVE));

        UserApiKeyResponse result = controller.updateApiKey(TEAM_ID, API_KEY_ID, request);

        assertThat(result).isNotNull();
    }

    @Test
    void deleteApiKey_success() {
        var result = controller.deleteApiKey(TEAM_ID, API_KEY_ID);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(userApiKeyService).delete(API_KEY_ID);
    }
}