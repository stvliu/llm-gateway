package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * ModelMatcher 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelMatcher 单元测试")
class ModelMatcherTest {

    @Mock
    private ModelSpecGateway modelSpecGateway;

    @InjectMocks
    private ModelMatcher modelMatcher;

    @Nested
    @DisplayName("match 模型匹配")
    class MatchTests {

        @Test
        @DisplayName("匹配成功 — 返回活跃的 ModelSpec")
        void match_activeModel_returnsModelSpec() {
            // given
            ModelSpec modelSpec = new ModelSpec();
            modelSpec.setId(1L);
            modelSpec.setProviderModelId("gpt-4o");
            modelSpec.setState(ModelSpecState.ACTIVE);

            when(modelSpecGateway.findByProviderModelId("gpt-4o"))
                    .thenReturn(Optional.of(modelSpec));

            // when
            ModelSpec result = modelMatcher.match("gpt-4o");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProviderModelId()).isEqualTo("gpt-4o");
            assertThat(result.getState()).isEqualTo(ModelSpecState.ACTIVE);
        }

        @Test
        @DisplayName("模型不存在时抛出 ResourceNotFoundException")
        void match_modelNotFound_throwsException() {
            // given
            when(modelSpecGateway.findByProviderModelId("non-existent"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelMatcher.match("non-existent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ModelSpec")
                    .hasMessageContaining("non-existent");
        }

        @Test
        @DisplayName("模型已禁用时抛出 ResourceNotFoundException")
        void match_disabledModel_throwsException() {
            // given
            ModelSpec modelSpec = new ModelSpec();
            modelSpec.setId(1L);
            modelSpec.setProviderModelId("gpt-4o");
            modelSpec.setState(ModelSpecState.DISABLED);

            when(modelSpecGateway.findByProviderModelId("gpt-4o"))
                    .thenReturn(Optional.of(modelSpec));

            // when & then
            assertThatThrownBy(() -> modelMatcher.match("gpt-4o"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ModelSpec")
                    .hasMessageContaining("gpt-4o");
        }
    }
}
