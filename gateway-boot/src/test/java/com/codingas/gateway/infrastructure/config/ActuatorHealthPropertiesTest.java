/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
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
