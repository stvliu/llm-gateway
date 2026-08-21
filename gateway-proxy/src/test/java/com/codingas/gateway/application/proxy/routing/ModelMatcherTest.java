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
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelGateway;
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
    private ModelGateway modelGateway;

    @InjectMocks
    private ModelMatcher modelMatcher;

    @Nested
    @DisplayName("match 模型匹配")
    class MatchTests {

        @Test
        @DisplayName("匹配成功 — 返回活跃的 Model")
        void match_activeModel_returnsModel() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            when(modelGateway.findByModelName("gpt-4o"))
                    .thenReturn(Optional.of(model));

            // when
            Model result = modelMatcher.match("gpt-4o");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getModelName()).isEqualTo("gpt-4o");
        }

        @Test
        @DisplayName("模型不存在时抛出 ResourceNotFoundException")
        void match_modelNotFound_throwsException() {
            // given
            when(modelGateway.findByModelName("non-existent"))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> modelMatcher.match("non-existent"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Model")
                    .hasMessageContaining("non-existent");
        }

        @Test
        @DisplayName("模型已禁用时抛出 ResourceNotFoundException")
        void match_disabledModel_throwsException() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");
            model.setDeprecatedAt(java.time.Instant.now());

            when(modelGateway.findByModelName("gpt-4o"))
                    .thenReturn(Optional.of(model));

            // when & then
            assertThatThrownBy(() -> modelMatcher.match("gpt-4o"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Model")
                    .hasMessageContaining("gpt-4o");
        }
    }
}
