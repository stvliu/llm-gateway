package com.codingas.gateway.infrastructure.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ProviderException;
import com.codingas.gateway.common.exception.SecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler 单元测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    // 用于 MethodParameter 创建的辅助方法
    @SuppressWarnings("unused")
    private void dummyMethod(String param) {}

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("GatewayRequestException 处理")
    class HandleGatewayRequestExceptionTests {

        @Test
        @DisplayName("应返回 BAD_REQUEST 状态码")
        void handleGatewayRequestException_returnsBadRequest() {
            // given
            GatewayRequestException ex = new GatewayRequestException("ERR_001", "Invalid request");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleGatewayRequestException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("ERR_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Invalid request");
        }
    }

    @Nested
    @DisplayName("ProviderException 处理")
    class HandleProviderExceptionTests {

        @Test
        @DisplayName("应返回 BAD_GATEWAY 状态码")
        void handleProviderException_returnsBadGateway() {
            // given
            ProviderException ex = new ProviderException("PROVIDER_001", "Provider unavailable");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleProviderException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("PROVIDER_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Provider unavailable");
        }
    }

    @Nested
    @DisplayName("SecurityException 处理")
    class HandleSecurityExceptionTests {

        @Test
        @DisplayName("应返回 FORBIDDEN 状态码")
        void handleSecurityException_returnsForbidden() {
            // given
            SecurityException ex = new SecurityException("SEC_001", "Access denied");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleSecurityException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("SEC_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Access denied");
        }
    }

    @Nested
    @DisplayName("GatewayException 处理")
    class HandleGatewayExceptionTests {

        @Test
        @DisplayName("应返回 INTERNAL_SERVER_ERROR 状态码")
        void handleGatewayException_returnsInternalServerError() {
            // given
            GatewayException ex = new GatewayException("GW_001", "Internal gateway error");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleGatewayException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("GW_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Internal gateway error");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException 处理")
    class HandleValidationExceptionTests {

        @Mock
        private BindingResult bindingResult;

        @Test
        @DisplayName("应提取字段错误消息并返回 BAD_REQUEST")
        void handleValidationException_extractsFieldErrors() throws NoSuchMethodException {
            // given
            FieldError fieldError1 = new FieldError("order", "amount", "Amount must be positive");
            FieldError fieldError2 = new FieldError("order", "customerName", "Customer name is required");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodParameter methodParameter = new MethodParameter(
                    GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyMethod", String.class), 0);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getError().getMessage())
                    .contains("Amount must be positive")
                    .contains("Customer name is required");
        }
    }

    @Nested
    @DisplayName("Exception 处理")
    class HandleGenericExceptionTests {

        @Test
        @DisplayName("应返回通用错误消息和 INTERNAL_SERVER_ERROR")
        void handleGenericException_returnsGenericError() {
            // given
            Exception ex = new RuntimeException("Some internal detail");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("An unexpected error occurred");
        }
    }
}
