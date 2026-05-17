package com.codingas.gateway.domain.model.service;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ApiKeySelectionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ApiKeySelectionServiceTest {

    @Mock
    private ProviderApiKeyGateway providerApiKeyGateway;

    @InjectMocks
    private ApiKeySelectionService apiKeySelectionService;

    private ProviderApiKey createApiKey(Long id, String name, Integer weight, Boolean isDefault, ProviderApiKeyState state) {
        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setId(id);
        apiKey.setKeyName(name);
        apiKey.setWeight(weight);
        apiKey.setIsDefault(isDefault);
        apiKey.setState(state);
        return apiKey;
    }

    @Nested
    @DisplayName("selectApiKey 方法测试")
    class SelectApiKeyTests {

        @Test
        @DisplayName("存在默认 Key 且可用时返回默认 Key")
        void selectApiKey_defaultKeyAvailable_returnsDefaultKey() {
            // Arrange
            ProviderApiKey defaultKey = createApiKey(1L, "default-key", 100, true, ProviderApiKeyState.ACTIVE);
            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.of(defaultKey));

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getIsDefault()).isTrue();
            verify(providerApiKeyGateway, never()).findActiveKeysByProviderId(anyLong());
        }

        @Test
        @DisplayName("默认 Key 存在但不可用时回退到加权选择")
        void selectApiKey_defaultKeyNotAvailable_fallbackToWeighted() {
            // Arrange
            ProviderApiKey defaultKey = createApiKey(1L, "default-key", 100, true, ProviderApiKeyState.DISABLED);
            ProviderApiKey activeKey1 = createApiKey(2L, "key1", 60, false, ProviderApiKeyState.ACTIVE);
            ProviderApiKey activeKey2 = createApiKey(3L, "key2", 40, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.of(defaultKey));
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(activeKey1, activeKey2));

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getState()).isEqualTo(ProviderApiKeyState.ACTIVE);
            verify(providerApiKeyGateway).findActiveKeysByProviderId(100L);
        }

        @Test
        @DisplayName("无默认 Key 时按权重选择")
        void selectApiKey_noDefaultKey_selectsByWeight() {
            // Arrange
            ProviderApiKey activeKey1 = createApiKey(1L, "key1", 70, false, ProviderApiKeyState.ACTIVE);
            ProviderApiKey activeKey2 = createApiKey(2L, "key2", 30, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(activeKey1, activeKey2));

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isIn(1L, 2L);
        }

        @Test
        @DisplayName("单个活跃 Key 时直接返回")
        void selectApiKey_singleActiveKey_returnsDirectly() {
            // Arrange
            ProviderApiKey activeKey = createApiKey(1L, "key1", 100, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(activeKey));

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("无可用 Key 时返回 null")
        void selectApiKey_noAvailableKeys_returnsNull() {
            // Arrange
            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of());

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("加权选择测试")
    class WeightedSelectionTests {

        @Test
        @DisplayName("按权重正确分配概率")
        void selectByWeight_correctDistribution() {
            // Arrange - 权重 70:30
            ProviderApiKey key1 = createApiKey(1L, "key1", 70, false, ProviderApiKeyState.ACTIVE);
            ProviderApiKey key2 = createApiKey(2L, "key2", 30, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(key1, key2));

            // Act - 多次调用统计分布
            int key1Count = 0;
            int key2Count = 0;
            int iterations = 1000;

            for (int i = 0; i < iterations; i++) {
                ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);
                if (result.getId() == 1L) key1Count++;
                else key2Count++;
            }

            // Assert - 概率应该接近 70:30（允许误差）
            double key1Ratio = (double) key1Count / iterations;
            assertThat(key1Ratio).isBetween(0.65, 0.75); // 允许 5% 误差
        }

        @Test
        @DisplayName("所有权重为 0 时随机选择")
        void selectByWeight_allWeightsZero_randomSelection() {
            // Arrange
            ProviderApiKey key1 = createApiKey(1L, "key1", 0, false, ProviderApiKeyState.ACTIVE);
            ProviderApiKey key2 = createApiKey(2L, "key2", 0, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(key1, key2));

            // Act
            ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isIn(1L, 2L);
        }

        @Test
        @DisplayName("weight 为 null 时使用默认值 100")
        void selectByWeight_nullWeight_usesDefault100() {
            // Arrange
            ProviderApiKey key1 = createApiKey(1L, "key1", null, false, ProviderApiKeyState.ACTIVE);
            ProviderApiKey key2 = createApiKey(2L, "key2", null, false, ProviderApiKeyState.ACTIVE);

            when(providerApiKeyGateway.findDefaultKeyByProviderId(100L)).thenReturn(Optional.empty());
            when(providerApiKeyGateway.findActiveKeysByProviderId(100L)).thenReturn(List.of(key1, key2));

            // Act - 多次调用验证两个 Key 都可能被选中
            boolean key1Selected = false;
            boolean key2Selected = false;

            for (int i = 0; i < 100; i++) {
                ProviderApiKey result = apiKeySelectionService.selectApiKey(100L);
                if (result.getId() == 1L) key1Selected = true;
                if (result.getId() == 2L) key2Selected = true;
            }

            // Assert - 两个 Key 都应该被选中过（50:50 概率）
            assertThat(key1Selected).isTrue();
            assertThat(key2Selected).isTrue();
        }
    }
}
