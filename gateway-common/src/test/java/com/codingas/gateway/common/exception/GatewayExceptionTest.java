package com.codingas.gateway.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GatewayException 单元测试
 */
@DisplayName("GatewayException 测试")
class GatewayExceptionTest {

    /**
     * 测试具体异常类能正常继承
     */
    @Test
    @DisplayName("子类能正常继承 GatewayException")
    void subclass_canExtendGatewayException() {
        TestException exception = new TestException("Test error message");
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("Test error message");
    }

    /**
     * 测试错误代码默认值为 GATEWAY_ERROR
     */
    @Test
    @DisplayName("默认错误代码为 GATEWAY_ERROR")
    void defaultErrorCode_isGATEWAYERROR() {
        TestException exception = new TestException("Test message");
        assertThat(exception.getErrorCode()).isEqualTo("GATEWAY_ERROR");
    }

    /**
     * 测试自定义错误代码
     */
    @Test
    @DisplayName("自定义错误代码正确设置")
    void customErrorCode_setCorrectly() {
        TestException exception = new TestException("INVALID_KEY", "Key is invalid");
        assertThat(exception.getErrorCode()).isEqualTo("INVALID_KEY");
        assertThat(exception.getMessage()).isEqualTo("Key is invalid");
    }

    /**
     * 测试带原因的异常
     */
    @Test
    @DisplayName("带原因的异常正确设置")
    void exceptionWithCause_setCorrectly() {
        RuntimeException cause = new RuntimeException("Original error");
        TestException exception = new TestException("PROCESSING_ERROR", "Processing failed", cause);

        assertThat(exception.getErrorCode()).isEqualTo("PROCESSING_ERROR");
        assertThat(exception.getMessage()).isEqualTo("Processing failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    /**
     * 测试异常可被抛出和捕获
     */
    @Test
    @DisplayName("异常可被正常抛出和捕获")
    void exception_canBeThrownAndCaught() {
        TestException exception = new TestException("Test error");

        assertThatThrownBy(() -> {
            throw exception;
        })
                .isInstanceOf(GatewayException.class)
                .hasMessage("Test error");
    }

    /**
     * 测试异常是 RuntimeException 的子类
     */
    @Test
    @DisplayName("GatewayException 是 RuntimeException 的子类")
    void gatewayException_extendsRuntimeException() {
        TestException exception = new TestException("Test");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    /**
     * 用于测试的 GatewayException 子类
     */
    static class TestException extends GatewayException {
        protected TestException(String message) {
            super(message);
        }

        protected TestException(String errorCode, String message) {
            super(errorCode, message);
        }

        protected TestException(String errorCode, String message, Throwable cause) {
            super(errorCode, message, cause);
        }
    }
}
