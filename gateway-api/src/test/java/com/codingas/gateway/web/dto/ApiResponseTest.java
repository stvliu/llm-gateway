package com.codingas.gateway.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ApiResponse 单元测试
 */
@DisplayName("ApiResponse Tests")
class ApiResponseTest {

    @Nested
    @DisplayName("success")
    class SuccessTests {

        @Test
        @DisplayName("创建带数据的成功响应")
        void success_withData() {
            ApiResponse<String> response = ApiResponse.success("test data");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("test data");
            assertThat(response.getError()).isNull();
            assertThat(response.getTraceId()).isNotNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("创建无数据的成功响应")
        void success_noData() {
            ApiResponse<Void> response = ApiResponse.success();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
            assertThat(response.getError()).isNull();
        }
    }

    @Nested
    @DisplayName("error")
    class ErrorTests {

        @Test
        @DisplayName("创建错误响应")
        void error_withCodeAndMessage() {
            ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Provider not found");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getData()).isNull();
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().getCode()).isEqualTo("NOT_FOUND");
            assertThat(response.getError().getMessage()).isEqualTo("Provider not found");
            assertThat(response.getTraceId()).isNotNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("创建错误响应后可以手动设置详情")
        void error_canSetDetailsManually() {
            ApiResponse<Void> response = ApiResponse.error("VALIDATION_ERROR", "Validation failed");
            response.getError().setDetails("Field 'name' is required");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getError().getMessage()).isEqualTo("Validation failed");
            assertThat(response.getError().getDetails()).isEqualTo("Field 'name' is required");
        }
    }

    @Nested
    @DisplayName("ErrorInfo")
    class ErrorInfoTests {

        @Test
        @DisplayName("ErrorInfo 构造函数设置正确")
        void errorInfo_constructor() {
            ApiResponse.ErrorInfo errorInfo = new ApiResponse.ErrorInfo("ERROR_CODE", "Error message");

            assertThat(errorInfo.getCode()).isEqualTo("ERROR_CODE");
            assertThat(errorInfo.getMessage()).isEqualTo("Error message");
            assertThat(errorInfo.getDetails()).isNull();
        }

        @Test
        @DisplayName("ErrorInfo 带详情的构造函数")
        void errorInfo_constructorWithDetails() {
            ApiResponse.ErrorInfo errorInfo = new ApiResponse.ErrorInfo("ERROR_CODE", "Error message", "Some details");

            assertThat(errorInfo.getCode()).isEqualTo("ERROR_CODE");
            assertThat(errorInfo.getMessage()).isEqualTo("Error message");
            assertThat(errorInfo.getDetails()).isEqualTo("Some details");
        }
    }
}