package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.*;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

/**
 * TeamController 中 UserApiKey 子资源端点单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamController UserApiKey 端点测试")
class TeamControllerUserApiKeyTest {

    @Mock
    private TeamService teamService;

    @Mock
    private UserApiKeyService userApiKeyService;

    @Mock
    private UserTeamGateway userTeamGateway;

    @Mock
    private TeamChannelGateway teamChannelGateway;

    @InjectMocks
    private TeamController controller;

    private static final Long TEAM_ID = 1L;
    private static final Long USER_ID = 50L;
    private static final Long API_KEY_ID = 100L;

    @Nested
    @DisplayName("查询团队 API Key 列表")
    class ListApiKeys {

        @Test
        @DisplayName("返回团队成员的所有 API Key")
        void returnsAllTeamMemberApiKeys() {
            UserTeam member1 = new UserTeam();
            member1.setUserId(USER_ID);
            member1.setTeamId(TEAM_ID);

            when(userTeamGateway.findByTeamId(TEAM_ID)).thenReturn(List.of(member1));

            UserApiKeyResponse keyResponse = new UserApiKeyResponse(
                    API_KEY_ID, USER_ID, "sk-abc", "sk-abc****bc1", "test-key",
                    List.of("gpt-4o"), 100000L, UserApiKeyState.ACTIVE,
                    Instant.now(), Instant.now()
            );
            when(userApiKeyService.findByUserId(USER_ID)).thenReturn(List.of(keyResponse));

            List<UserApiKeyResponse> result = controller.listApiKeys(TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(API_KEY_ID);
        }

        @Test
        @DisplayName("团队无成员时返回空列表")
        void returnsEmptyWhenNoMembers() {
            when(userTeamGateway.findByTeamId(TEAM_ID)).thenReturn(List.of());

            List<UserApiKeyResponse> result = controller.listApiKeys(TEAM_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("查询 API Key 详情")
    class GetApiKey {

        @Test
        @DisplayName("返回 API Key 详情")
        void returnsDetail() {
            UserApiKeyDetailResponse detail = new UserApiKeyDetailResponse(
                    API_KEY_ID, USER_ID, "sk-abc", "sk-abc1xxxxx", "test-key",
                    List.of("gpt-4o"), 100000L, UserApiKeyState.ACTIVE,
                    Instant.now(), Instant.now()
            );
            when(userApiKeyService.getDetailById(API_KEY_ID)).thenReturn(detail);

            UserApiKeyDetailResponse result = controller.getApiKey(API_KEY_ID);

            assertThat(result.id()).isEqualTo(API_KEY_ID);
            assertThat(result.keyPlain()).isEqualTo("sk-abc1xxxxx");
        }
    }

    @Nested
    @DisplayName("更新 API Key")
    class UpdateApiKey {

        @Test
        @DisplayName("更新 API Key 并返回结果")
        void updatesAndReturns() {
            UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                    "updated-name", List.of("claude-3-5-sonnet"), null, null
            );
            UserApiKeyResponse updated = new UserApiKeyResponse(
                    API_KEY_ID, USER_ID, "sk-abc", "sk-abc****bc1", "updated-name",
                    List.of("claude-3-5-sonnet"), 100000L, UserApiKeyState.ACTIVE,
                    Instant.now(), Instant.now()
            );
            when(userApiKeyService.update(eq(API_KEY_ID), any(UserApiKeyUpdateRequest.class)))
                    .thenReturn(updated);

            UserApiKeyResponse result = controller.updateApiKey(API_KEY_ID, request);

            assertThat(result.name()).isEqualTo("updated-name");
            assertThat(result.models()).containsExactly("claude-3-5-sonnet");
        }
    }

    @Nested
    @DisplayName("删除 API Key")
    class DeleteApiKey {

        @Test
        @DisplayName("调用删除服务方法")
        void callsDelete() {
            controller.deleteApiKey(API_KEY_ID);
            verify(userApiKeyService).delete(API_KEY_ID);
        }
    }
}