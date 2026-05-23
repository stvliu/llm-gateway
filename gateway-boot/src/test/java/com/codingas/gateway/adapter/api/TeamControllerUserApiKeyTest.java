package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeamController 中 UserApiKey 端点单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamController UserApiKey 端点测试")
class TeamControllerUserApiKeyTest {

    @Mock
    private TeamService teamService;

    @Mock
    private UserApiKeyService userApiKeyService;

    @Mock
    private HttpServletRequest httpRequest;

    private TeamController controller;

    private static final Long TEAM_ID = 1L;
    private static final Long USER_ID = 50L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long API_KEY_ID = 100L;

    @BeforeEach
    void setUp() {
        controller = new TeamController(teamService, userApiKeyService);
    }

    @Test
    @DisplayName("查询团队密钥列表")
    void listApiKeys_success() {
        when(userApiKeyService.listByTeamId(TEAM_ID))
                .thenReturn(List.of());

        List<UserApiKeyResponse> result = controller.listApiKeys(TEAM_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("查询密钥详情")
    void getApiKey_success() {
        UserApiKeyDetailResponse detailResponse = new UserApiKeyDetailResponse(
                API_KEY_ID, TEAM_ID, USER_ID, List.of(PRODUCT_ID), List.of(),
                "sk-abc1", "sk-abc1xxxxx", "test-key",
                List.of("gpt-4o"), 100000L, UserApiKeyState.ACTIVE,
                Instant.now(), Instant.now()
        );
        when(userApiKeyService.getDetailByIdAndTeamId(API_KEY_ID, TEAM_ID)).thenReturn(detailResponse);

        UserApiKeyDetailResponse result = controller.getApiKey(TEAM_ID, API_KEY_ID);

        assertThat(result.id()).isEqualTo(API_KEY_ID);
        assertThat(result.keyPlain()).isEqualTo("sk-abc1xxxxx");
    }

    @Test
    @DisplayName("创建密钥")
    void createApiKey_success() {
        try (MockedStatic<StpUtil> stpUtilMock = org.mockito.Mockito.mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
            stpUtilMock.when(() -> StpUtil.hasRole("ADMIN")).thenReturn(false);
            UserApiKeyCreateRequest request = new UserApiKeyCreateRequest(
                    TEAM_ID, USER_ID, List.of(PRODUCT_ID), "test-key", List.of("gpt-4o"), 100000L
            );
            UserApiKeyCreateResponse createResponse = new UserApiKeyCreateResponse(
                    API_KEY_ID, "sk-abc1", "sk-abc1xxxxx"
            );
            when(userApiKeyService.create(any(UserApiKeyCreateRequest.class)))
                    .thenReturn(createResponse);

            var result = controller.createApiKey(TEAM_ID, request);

            assertThat(result.getStatusCode().value()).isEqualTo(201);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().id()).isEqualTo(API_KEY_ID);
            verify(userApiKeyService).create(any(UserApiKeyCreateRequest.class));
        }
    }

    @Test
    @DisplayName("更新密钥")
    void updateApiKey_success() {
        UserApiKeyUpdateRequest request = new UserApiKeyUpdateRequest(
                "updated-name", List.of(PRODUCT_ID), List.of("claude-3-5-sonnet"), null, null
        );
        UserApiKeyResponse updateResponse = new UserApiKeyResponse(
                API_KEY_ID, TEAM_ID, USER_ID, List.of(PRODUCT_ID), List.of(),
                "sk-abc1", "updated-name",
                List.of("claude-3-5-sonnet"), 100000L, UserApiKeyState.ACTIVE,
                Instant.now(), Instant.now()
        );
        when(userApiKeyService.update(any(), any(UserApiKeyUpdateRequest.class)))
                .thenReturn(updateResponse);

        UserApiKeyResponse result = controller.updateApiKey(TEAM_ID, API_KEY_ID, request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("删除密钥")
    void deleteApiKey_success() {
        var result = controller.deleteApiKey(TEAM_ID, API_KEY_ID);

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(userApiKeyService).delete(API_KEY_ID);
    }
}
