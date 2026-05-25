package com.codingas.gateway.domain.iam.service;

import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyGenerator 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyGenerator 测试")
class ApiKeyGeneratorTest {

    @Mock
    private UserApiKeyGateway userApiKeyGateway;

    private UserApiKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultUserApiKeyGenerator(userApiKeyGateway);
    }

    @Nested
    @DisplayName("generate 方法测试")
    class GenerateTests {

        @Test
        @DisplayName("生成成功")
        void generate_success() {
            when(userApiKeyGateway.findByKeyPrefix(anyString())).thenReturn(Optional.empty());

            GeneratedApiKey result = generator.generate();

            assertThat(result.plainKey()).startsWith("sk-");
            assertThat(result.keyPrefix()).hasSize(10);
            assertThat(result.keyPrefix()).startsWith("sk-");
        }

        @Test
        @DisplayName("prefix 碰撞时重试成功")
        void generate_collision_retrySuccess() {
            UserApiKey existing = new UserApiKey();
            when(userApiKeyGateway.findByKeyPrefix(anyString()))
                    .thenReturn(Optional.of(existing))
                    .thenReturn(Optional.empty());

            GeneratedApiKey result = generator.generate();

            assertThat(result).isNotNull();
            assertThat(result.plainKey()).startsWith("sk-");
            verify(userApiKeyGateway, times(2)).findByKeyPrefix(anyString());
        }

        @Test
        @DisplayName("prefix 碰撞超过重试次数抛异常")
        void generate_collision_exceedRetries() {
            UserApiKey existing = new UserApiKey();
            when(userApiKeyGateway.findByKeyPrefix(anyString()))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> generator.generate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("无法生成唯一的 API Key");

            verify(userApiKeyGateway, times(5)).findByKeyPrefix(anyString());
        }
    }
}
