package com.codingas.gateway.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 FullContextIntegrationTestBase 能正常加载 Spring 上下文
 */
class FullContextIntegrationTest extends FullContextIntegrationTestBase {

    @Test
    void contextLoads() {
        assertThat(chatDispatchService).isNotNull();
    }
}
