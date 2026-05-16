package com.codingas.gateway.application.metadata;

import com.codingas.gateway.application.metadata.dto.ApplyMetadataRequest;
import com.codingas.gateway.application.metadata.dto.ApplyMetadataResult;
import com.codingas.gateway.application.metadata.dto.MetadataCreateRequest;
import com.codingas.gateway.application.metadata.dto.MetadataUpdateRequest;
import com.codingas.gateway.application.metadata.dto.ProviderMetadataResponse;
import com.codingas.gateway.domain.metadata.entity.MarketState;
import com.codingas.gateway.domain.metadata.entity.MetadataType;
import com.codingas.gateway.domain.metadata.entity.ModelMetadata;
import com.codingas.gateway.domain.metadata.entity.ProviderMetadata;
import com.codingas.gateway.domain.metadata.gateway.ModelMetadataGateway;
import com.codingas.gateway.domain.metadata.gateway.ProviderMetadataGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 供应商元数据服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProviderMetadataServiceTest {

    @Mock
    private ProviderMetadataGateway providerMetadataGateway;

    @Mock
    private ModelMetadataGateway modelMetadataGateway;

    private ProviderMetadataService service;

    @BeforeEach
    void setUp() {
        service = new ProviderMetadataService(providerMetadataGateway, modelMetadataGateway);
    }

    @Nested
    @DisplayName("获取供应商元数据详情")
    class GetTests {

        @Test
        @DisplayName("存在时返回详情")
        void existingId_returnsDetail() {
            ProviderMetadata metadata = buildProviderMetadata(1L, "openai", "OpenAI");
            when(providerMetadataGateway.findById(1L)).thenReturn(Optional.of(metadata));
            when(modelMetadataGateway.findByProviderId("openai")).thenReturn(List.of());

            ProviderMetadataResponse response = service.getProviderMetadata(1L);

            assertThat(response.getProviderId()).isEqualTo("openai");
            assertThat(response.getProviderName()).isEqualTo("OpenAI");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void nonExistingId_throwsException() {
            when(providerMetadataGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getProviderMetadata(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("创建供应商元数据")
    class CreateTests {

        @Test
        @DisplayName("providerId 不重复时创建成功")
        void uniqueProviderId_createsSuccessfully() {
            MetadataCreateRequest request = new MetadataCreateRequest();
            request.setProviderId("custom");
            request.setProviderName("Custom Provider");
            request.setProviderType("OPENAI");

            when(providerMetadataGateway.existsByProviderId("custom")).thenReturn(false);
            when(providerMetadataGateway.save(any(ProviderMetadata.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(modelMetadataGateway.findByProviderId("custom")).thenReturn(List.of());

            ProviderMetadataResponse response = service.createMetadata(request);

            assertThat(response.getProviderId()).isEqualTo("custom");
            assertThat(response.getMetadataType()).isEqualTo("USER");
            verify(providerMetadataGateway).save(any(ProviderMetadata.class));
        }

        @Test
        @DisplayName("providerId 重复时抛出异常")
        void duplicateProviderId_throwsException() {
            MetadataCreateRequest request = new MetadataCreateRequest();
            request.setProviderId("openai");
            request.setProviderName("OpenAI");
            request.setProviderType("OPENAI");

            when(providerMetadataGateway.existsByProviderId("openai")).thenReturn(true);

            assertThatThrownBy(() -> service.createMetadata(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openai");
            verify(providerMetadataGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("更新供应商元数据")
    class UpdateTests {

        @Test
        @DisplayName("存在时更新成功")
        void existingId_updatesSuccessfully() {
            ProviderMetadata metadata = buildProviderMetadata(1L, "openai", "OpenAI");
            MetadataUpdateRequest request = new MetadataUpdateRequest();
            request.setProviderName("OpenAI Updated");

            when(providerMetadataGateway.findById(1L)).thenReturn(Optional.of(metadata));
            when(providerMetadataGateway.save(any(ProviderMetadata.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(modelMetadataGateway.findByProviderId("openai")).thenReturn(List.of());

            ProviderMetadataResponse response = service.updateMetadata(1L, request);

            assertThat(response.getProviderName()).isEqualTo("OpenAI Updated");
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void nonExistingId_throwsException() {
            MetadataUpdateRequest request = new MetadataUpdateRequest();
            when(providerMetadataGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMetadata(99L, request))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("删除供应商元数据")
    class DeleteTests {

        @Test
        @DisplayName("调用 gateway 逻辑删除")
        void deletes_callsGateway() {
            service.deleteMetadata(1L);
            verify(providerMetadataGateway).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("更新市场状态")
    class MarketStatusTests {

        @Test
        @DisplayName("调用 gateway 更新市场状态")
        void updatesMarketStatus_callsGateway() {
            service.updateMarketStatus(1L, MarketState.PUBLISHED);
            verify(providerMetadataGateway).updateMarketStatus(1L, MarketState.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("应用元数据")
    class ApplyTests {

        @Test
        @DisplayName("存在时返回应用结果")
        void existingId_returnsApplyResult() {
            ProviderMetadata metadata = buildProviderMetadata(1L, "openai", "OpenAI");
            ModelMetadata model1 = new ModelMetadata("openai", "gpt-4.1", "GPT-4.1", com.codingas.gateway.domain.metadata.entity.MetadataSource.MODELS_DEV);
            model1.setId(10L);

            ApplyMetadataRequest request = new ApplyMetadataRequest();
            request.setApiKey("sk-test");

            when(providerMetadataGateway.findById(1L)).thenReturn(Optional.of(metadata));
            when(modelMetadataGateway.findByProviderId("openai")).thenReturn(List.of(model1));

            ApplyMetadataResult result = service.applyMetadata(1L, request);

            assertThat(result.getProviderName()).isEqualTo("OpenAI");
            assertThat(result.getModelIds()).containsExactly(10L);
            assertThat(result.getModelNames()).containsExactly("GPT-4.1");
            verify(providerMetadataGateway).incrementDownloadCount(1L);
        }

        @Test
        @DisplayName("不存在时抛出异常")
        void nonExistingId_throwsException() {
            ApplyMetadataRequest request = new ApplyMetadataRequest();
            when(providerMetadataGateway.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.applyMetadata(99L, request))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("获取官方元数据")
    class OfficialTests {

        @Test
        @DisplayName("返回所有官方元数据")
        void returnsOfficialMetadata() {
            ProviderMetadata official = buildProviderMetadata(1L, "openai", "OpenAI");
            official.setMetadataType(MetadataType.OFFICIAL);

            when(providerMetadataGateway.findOfficialMetadata()).thenReturn(List.of(official));
            when(modelMetadataGateway.findByProviderId("openai")).thenReturn(List.of());

            List<ProviderMetadataResponse> result = service.listOfficialMetadata();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMetadataType()).isEqualTo("OFFICIAL");
        }
    }

    private ProviderMetadata buildProviderMetadata(Long id, String providerId, String name) {
        ProviderMetadata metadata = new ProviderMetadata();
        metadata.setId(id);
        metadata.setProviderId(providerId);
        metadata.setProviderName(name);
        metadata.setProviderType("OPENAI");
        metadata.setMetadataType(MetadataType.OFFICIAL);
        metadata.setMarketState(MarketState.PRIVATE);
        metadata.setDownloadCount(0);
        metadata.setState(com.codingas.gateway.domain.metadata.enums.MetadataState.ACTIVE);
        return metadata;
    }
}