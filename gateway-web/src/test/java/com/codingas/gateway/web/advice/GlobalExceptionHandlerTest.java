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
package com.codingas.gateway.web.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.protocol.transport.UpstreamException;
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
 *
 * <p>注意：IamException/ThreatException 由各自的 ExceptionHandler 处理，不在本测试范围内。</p>
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
    @DisplayName("UpstreamException 处理")
    class HandleUpstreamExceptionTests {

        @Test
        @DisplayName("应返回 BAD_GATEWAY 状态码")
        void handleUpstreamException_returnsBadGateway() {
            // given
            UpstreamException ex = new UpstreamException("PROVIDER_001", "Provider unavailable");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleUpstreamException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("PROVIDER_001");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Provider unavailable");
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
    @DisplayName("ResourceNotFoundException 处理")
    class HandleResourceNotFoundExceptionTests {

        @Test
        @DisplayName("应返回 NOT_FOUND 状态码并透出业务消息")
        void handleResourceNotFoundException_returnsNotFound() {
            // given
            ResourceNotFoundException ex = new ResourceNotFoundException("Model", 1L);

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Model not found with id: 1");
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException 处理")
    class HandleDuplicateResourceExceptionTests {

        @Test
        @DisplayName("应返回 CONFLICT 状态码并透出业务消息")
        void handleDuplicateResourceException_returnsConflict() {
            // given
            DuplicateResourceException ex = new DuplicateResourceException("Model", "modelName");

            // when
            ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicateResourceException(ex);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().getCode()).isEqualTo("CONFLICT");
            assertThat(response.getBody().getError().getMessage()).isEqualTo("Model already exists with modelName");
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
