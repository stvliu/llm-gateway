package com.codingas.gateway.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * SecurityException 单元测试
 */
@DisplayName("SecurityException Tests")
class SecurityExceptionTest {

    @Nested
    @DisplayName("构造")
    class ConstructorTests {

        @Test
        @DisplayName("构造器设置正确错误码和消息")
        void constructor_setsCorrectErrorCodeAndMessage() {
            SecurityException ex = new SecurityException("Access denied");

            assertThat(ex.getMessage()).isEqualTo("Access denied");
            assertThat(ex.getErrorCode()).isEqualTo("SECURITY_ERROR");
        }

        @Test
        @DisplayName("带根因构造器正确设置")
        void constructorWithCause_setsCorrectCause() {
            RuntimeException cause = new RuntimeException("Auth server error");
            SecurityException ex = new SecurityException("Authentication failed", cause);

            assertThat(ex.getMessage()).isEqualTo("Authentication failed");
            assertThat(ex.getErrorCode()).isEqualTo("SECURITY_ERROR");
            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("继承关系")
    class InheritanceTests {

        @Test
        @DisplayName("继承自 GatewayException")
        void extendsGatewayException() {
            SecurityException ex = new SecurityException("Test");

            assertThat(ex).isInstanceOf(GatewayException.class);
        }
    }
}