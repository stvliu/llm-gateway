package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.ApplyTemplateRequest;
import com.codingas.gateway.application.template.dto.ApplyTemplateResult;
import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderTemplateService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ProviderTemplateServiceTest {

    @Mock
    private ProviderTemplateGateway providerTemplateGateway;

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private ProviderApiKeyGateway providerApiKeyGateway;

    @Mock
    private ObjectMapper objectMapper;

    private ProviderTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ProviderTemplateService(
            providerTemplateGateway,
            providerGateway,
            modelGateway,
            providerApiKeyGateway,
            objectMapper
        );
    }

    @Test
    @DisplayName("创建自定义模板成功")
    void createTemplate_success() {
        TemplateCreateRequest request = createTestRequest();
        when(providerTemplateGateway.existsByTemplateCode("test-provider")).thenReturn(false);
        when(providerTemplateGateway.save(any())).thenAnswer(inv -> {
            ProviderTemplate t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TemplateResponse response = service.createTemplate(request, 1L, "testuser");

        assertThat(response).isNotNull();
        assertThat(response.getTemplateCode()).isEqualTo("test-provider");
        assertThat(response.getTemplateType()).isEqualTo(TemplateType.USER);
        verify(providerTemplateGateway).save(any());
    }

    @Test
    @DisplayName("创建模板时编码重复抛出异常")
    void createTemplate_duplicateCode_throws() {
        TemplateCreateRequest request = createTestRequest();
        when(providerTemplateGateway.existsByTemplateCode("test-provider")).thenReturn(true);

        assertThatThrownBy(() -> service.createTemplate(request, 1L, "testuser"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("模板编码已存在");
    }

    @Test
    @DisplayName("更新模板成功")
    void updateTemplate_success() {
        ProviderTemplate existing = createTestTemplate();
        existing.setId(1L);
        existing.setTemplateType(TemplateType.USER);

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("Updated Name");

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(existing));
        when(providerTemplateGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateResponse response = service.updateTemplate(1L, request);

        assertThat(response.getTemplateName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("更新官方模板抛出异常")
    void updateTemplate_officialTemplate_throws() {
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(official));

        assertThatThrownBy(() -> service.updateTemplate(1L, new TemplateUpdateRequest()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板不允许修改");
    }

    @Test
    @DisplayName("删除模板成功")
    void deleteTemplate_success() {
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(template));
        doNothing().when(providerTemplateGateway).deleteById(1L);

        service.deleteTemplate(1L);

        verify(providerTemplateGateway).deleteById(1L);
    }

    @Test
    @DisplayName("删除官方模板抛出异常")
    void deleteTemplate_officialTemplate_throws() {
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(official));

        assertThatThrownBy(() -> service.deleteTemplate(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板不允许删除");
    }

    @Test
    @DisplayName("发布模板到公共市场")
    void publishTemplate_success() {
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);
        template.setMarketStatus(MarketStatus.PRIVATE);

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(template));
        when(providerTemplateGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.publishTemplate(1L);

        verify(providerTemplateGateway).save(any());
    }

    @Test
    @DisplayName("发布官方模板抛出异常")
    void publishTemplate_officialTemplate_throws() {
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(official));

        assertThatThrownBy(() -> service.publishTemplate(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板无需发布");
    }

    @Test
    @DisplayName("应用模板创建 Provider、Model、ApiKey")
    void applyTemplate_success() {
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);

        ApplyTemplateRequest request = new ApplyTemplateRequest();
        request.setApiKey("sk-test-key");

        when(providerTemplateGateway.findById(1L)).thenReturn(Optional.of(template));
        when(providerGateway.save(any())).thenAnswer(inv -> {
            Provider p = inv.getArgument(0);
            p.setId(100L);
            p.setCreatedAt(Instant.now());
            return p;
        });
        when(modelGateway.save(any())).thenAnswer(inv -> {
            Model m = inv.getArgument(0);
            m.setId(300L);
            m.setCreatedAt(Instant.now());
            return m;
        });
        when(providerApiKeyGateway.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(providerTemplateGateway).incrementDownloadCount(1L);

        ApplyTemplateResult result = service.applyTemplate(1L, request, 1L);

        assertThat(result.getProviderId()).isEqualTo(100L);
        assertThat(result.getModelIds()).hasSize(1);
        assertThat(result.getModelIds().get(0)).isEqualTo(300L);

        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        verify(providerGateway).save(providerCaptor.capture());
        Provider savedProvider = providerCaptor.getValue();
        assertThat(savedProvider.getName()).isEqualTo("Test Provider");
        assertThat(savedProvider.getType().name()).isEqualTo("OTHER");

        ArgumentCaptor<ProviderApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ProviderApiKey.class);
        verify(providerApiKeyGateway).save(apiKeyCaptor.capture());
        ProviderApiKey savedApiKey = apiKeyCaptor.getValue();
        assertThat(savedApiKey.getApiKey()).isEqualTo("sk-test-key");
        verify(providerTemplateGateway).incrementDownloadCount(1L);
    }

    private TemplateCreateRequest createTestRequest() {
        TemplateCreateRequest request = new TemplateCreateRequest();
        request.setTemplateCode("test-provider");
        request.setTemplateName("Test Provider");
        request.setProviderType("OTHER");
        request.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        request.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));
        return request;
    }

    private ProviderTemplate createTestTemplate() {
        ProviderTemplate template = new ProviderTemplate();
        template.setTemplateCode("test-provider");
        template.setTemplateName("Test Provider");
        template.setProviderType("OTHER");
        template.setProviderConfig(Map.of("base_url", "https://api.test.com"));
        template.setModelsConfig(List.of(Map.of("provider_model_id", "model-1", "display_name", "Model 1")));
        return template;
    }
}