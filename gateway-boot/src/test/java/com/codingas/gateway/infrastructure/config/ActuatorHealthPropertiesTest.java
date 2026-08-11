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
package com.codingas.gateway.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActuatorHealthProperties 测试")
class ActuatorHealthPropertiesTest {

    @Test
    @DisplayName("默认 publicAccess 为 true")
    void defaultPublicAccess_isTrue() {
        var props = new ActuatorHealthProperties();
        assertThat(props.isPublicAccess()).isTrue();
    }

    @Test
    @DisplayName("可以设置 publicAccess 为 false")
    void setPublicAccess_toFalse() {
        var props = new ActuatorHealthProperties();
        props.setPublicAccess(false);
        assertThat(props.isPublicAccess()).isFalse();
    }
}
