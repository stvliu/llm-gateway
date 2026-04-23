package com.codingas.gateway.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * GatewayException 单元测试
 */
@DisplayName("GatewayException Tests")
class GatewayExceptionTest {

    @Nested
    @DisplayName("异常构造")
    class ConstructorTests {

        @Test
        @DisplayName("默认构造设置默认错误码")
        void defaultConstructor_setsDefaultErrorCode() {
            TestGatewayException ex = new TestGatewayException("Test message");

            assertThat(ex.getMessage()).isEqualTo("Test message");
            assertThat(ex.getErrorCode()).isEqualTo("GATEWAY_ERROR");
        }

        @Test
        @DisplayName("带错误码构造正确设置")
        void withErrorCode_setsCorrectErrorCode() {
            TestGatewayException ex = new TestGatewayException("CUSTOM_ERROR", "Custom message");

            assertThat(ex.getMessage()).isEqualTo("Custom message");
            assertThat(ex.getErrorCode()).isEqualTo("CUSTOM_ERROR");
        }

        @Test
        @DisplayName("带根因构造正确设置")
        void withCause_setsCorrectCause() {
            RuntimeException cause = new RuntimeException("Original error");
            TestGatewayException ex = new TestGatewayException("ERROR_CODE", "Error message", cause);

            assertThat(ex.getMessage()).isEqualTo("Error message");
            assertThat(ex.getErrorCode()).isEqualTo("ERROR_CODE");
            assertThat(ex.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(ex.getCause().getMessage()).isEqualTo("Original error");
        }
    }

    @Nested
    @DisplayName("继承关系")
    class InheritanceTests {

        @Test
        @DisplayName("继承自 RuntimeException")
        void extendsRuntimeException() {
            TestGatewayException ex = new TestGatewayException("Test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }

    /**
     * 测试用子类
     */
    private static class TestGatewayException extends GatewayException {
        public TestGatewayException(String message) {
            super(message);
        }

        public TestGatewayException(String errorCode, String message) {
            super(errorCode, message);
        }

        public TestGatewayException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }
}