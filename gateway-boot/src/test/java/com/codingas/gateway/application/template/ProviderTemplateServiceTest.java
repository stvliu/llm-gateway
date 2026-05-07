package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ProviderTemplateGateway gateway;

    private ProviderTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ProviderTemplateService(gateway);
    }

    @Test
    @DisplayName("创建自定义模板成功")
    void createTemplate_success() {
        // Arrange
        TemplateCreateRequest request = createTestRequest();
        when(gateway.existsByTemplateCode("test-provider")).thenReturn(false);
        when(gateway.save(any())).thenAnswer(inv -> {
            ProviderTemplate t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        // Act
        TemplateResponse response = service.createTemplate(request, 1L, "testuser");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTemplateCode()).isEqualTo("test-provider");
        assertThat(response.getTemplateType()).isEqualTo(TemplateType.USER);
        verify(gateway).save(any());
    }

    @Test
    @DisplayName("创建模板时编码重复抛出异常")
    void createTemplate_duplicateCode_throws() {
        // Arrange
        TemplateCreateRequest request = createTestRequest();
        when(gateway.existsByTemplateCode("test-provider")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.createTemplate(request, 1L, "testuser"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("模板编码已存在");
    }

    @Test
    @DisplayName("更新模板成功")
    void updateTemplate_success() {
        // Arrange
        ProviderTemplate existing = createTestTemplate();
        existing.setId(1L);
        existing.setTemplateType(TemplateType.USER);

        TemplateUpdateRequest request = new TemplateUpdateRequest();
        request.setTemplateName("Updated Name");

        when(gateway.findById(1L)).thenReturn(Optional.of(existing));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        TemplateResponse response = service.updateTemplate(1L, request);

        // Assert
        assertThat(response.getTemplateName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("更新官方模板抛出异常")
    void updateTemplate_officialTemplate_throws() {
        // Arrange
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(gateway.findById(1L)).thenReturn(Optional.of(official));

        // Act & Assert
        assertThatThrownBy(() -> service.updateTemplate(1L, new TemplateUpdateRequest()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板不允许修改");
    }

    @Test
    @DisplayName("删除模板成功")
    void deleteTemplate_success() {
        // Arrange
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);

        when(gateway.findById(1L)).thenReturn(Optional.of(template));
        doNothing().when(gateway).deleteById(1L);

        // Act
        service.deleteTemplate(1L);

        // Assert
        verify(gateway).deleteById(1L);
    }

    @Test
    @DisplayName("删除官方模板抛出异常")
    void deleteTemplate_officialTemplate_throws() {
        // Arrange
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(gateway.findById(1L)).thenReturn(Optional.of(official));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteTemplate(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板不允许删除");
    }

    @Test
    @DisplayName("发布模板到公共市场")
    void publishTemplate_success() {
        // Arrange
        ProviderTemplate template = createTestTemplate();
        template.setId(1L);
        template.setTemplateType(TemplateType.USER);
        template.setMarketStatus(MarketStatus.PRIVATE);

        when(gateway.findById(1L)).thenReturn(Optional.of(template));
        when(gateway.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.publishTemplate(1L);

        // Assert
        verify(gateway).save(any());
    }

    @Test
    @DisplayName("发布官方模板抛出异常")
    void publishTemplate_officialTemplate_throws() {
        // Arrange
        ProviderTemplate official = createTestTemplate();
        official.setId(1L);
        official.setTemplateType(TemplateType.OFFICIAL);

        when(gateway.findById(1L)).thenReturn(Optional.of(official));

        // Act & Assert
        assertThatThrownBy(() -> service.publishTemplate(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("官方模板无需发布");
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
        template.setModelsConfig(List.of(Map.of("provider_model_id", "model-1")));
        return template;
    }
}
