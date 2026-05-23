package com.codingas.gateway.domain.proxy.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolValidationExceptionTest {

    @Test
    void shouldContainProtocolFieldAndViolation() {
        var ex = new ProtocolValidationException("anthropic", "max_tokens", "必填且大于0");
        assertThat(ex.getProtocol()).isEqualTo("anthropic");
        assertThat(ex.getField()).isEqualTo("max_tokens");
        assertThat(ex.getViolation()).isEqualTo("必填且大于0");
        assertThat(ex.getCode()).isEqualTo("PROTOCOL_VALIDATION_ERROR");
        assertThat(ex.getMessage()).contains("anthropic").contains("max_tokens");
    }
}
