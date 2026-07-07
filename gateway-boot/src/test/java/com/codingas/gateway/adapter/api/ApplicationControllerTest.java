package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.application.ApplicationService;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * ApplicationController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。
 * 应用是权限+行为双聚合根，本测试覆盖 Task 6 的 GET /api-keys 与 Task 4 的 DELETE 冲突前置校验。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationController 测试")
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private UserApiKeyService userApiKeyService;

    @InjectMocks
    private ApplicationController controller;

    @Nested
    @DisplayName("listApiKeys 方法测试")
    class ListApiKeysTests {

        @Test
        @DisplayName("查询应用下的 Key 列表 — 返回非空且 applicationId 匹配")
        void listApiKeys_success_returnsKeyList() {
            // given — 应用 7 下挂一个 Key
            UserApiKeyResponse key = new UserApiKeyResponse(
                    100L, 50L, 7L, "sk-abc1", null, "test-key", null, null);
            when(userApiKeyService.findByApplicationId(7L)).thenReturn(List.of(key));

            // when
            List<UserApiKeyResponse> result = controller.listApiKeys(7L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).applicationId()).isEqualTo(7L);
            assertThat(result.get(0).keyPrefix()).isEqualTo("sk-abc1");
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("应用下有 Key 引用时删除 — 抛 GatewayRequestException(APPLICATION_HAS_API_KEYS)")
        void delete_hasApiKeys_throwsConflict() {
            // given — ApplicationServiceImpl.delete 前置校验 UserApiKey 引用，有则抛冲突
            doThrow(new GatewayRequestException("APPLICATION_HAS_API_KEYS",
                    "应用下还有 API Key，请先转移或删除"))
                    .when(applicationService).delete(7L);

            // when & then — Controller 透传业务异常，由 GlobalExceptionHandler 映射 400
            assertThatThrownBy(() -> controller.delete(7L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("API Key");
        }
    }
}
