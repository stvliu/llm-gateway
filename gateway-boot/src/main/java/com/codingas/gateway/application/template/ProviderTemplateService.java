package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.TemplateCreateRequest;
import com.codingas.gateway.application.template.dto.TemplateResponse;
import com.codingas.gateway.application.template.dto.TemplateUpdateRequest;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Provider 模板应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTemplateService {

    private final ProviderTemplateGateway gateway;

    /**
     * 创建自定义模板
     */
    @Transactional
    public TemplateResponse createTemplate(TemplateCreateRequest request, Long userId, String username) {
        if (gateway.existsByTemplateCode(request.getTemplateCode())) {
            throw new IllegalArgumentException("模板编码已存在: " + request.getTemplateCode());
        }

        ProviderTemplate template = new ProviderTemplate();
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setTemplateType(TemplateType.USER);
        template.setProviderType(request.getProviderType());
        template.setProviderConfig(request.getProviderConfig());
        template.setModelsConfig(request.getModelsConfig());
        template.setDescription(request.getDescription());
        template.setIconUrl(request.getIconUrl());
        template.setTags(request.getTags());
        template.setAuthorId(userId);
        template.setAuthorName(username);
        template.setMarketStatus(MarketStatus.PRIVATE);
        template.setDownloadCount(0);

        ProviderTemplate saved = gateway.save(template);
        return toResponse(saved);
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateUpdateRequest request) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板不允许修改");
        }

        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getProviderConfig() != null) {
            template.setProviderConfig(request.getProviderConfig());
        }
        if (request.getModelsConfig() != null) {
            template.setModelsConfig(request.getModelsConfig());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            template.setIconUrl(request.getIconUrl());
        }
        if (request.getTags() != null) {
            template.setTags(request.getTags());
        }

        ProviderTemplate saved = gateway.save(template);
        return toResponse(saved);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板不允许删除");
        }

        gateway.deleteById(id);
    }

    /**
     * 查询模板详情
     */
    public TemplateResponse getTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        return toResponse(template);
    }

    /**
     * 分页查询模板
     */
    public Page<TemplateResponse> listTemplates(
            TemplateType type,
            String providerType,
            String keyword,
            MarketStatus marketStatus,
            int page,
            int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<ProviderTemplate> result = gateway.findByConditions(type, providerType, keyword, marketStatus, pageable);
        return result.map(this::toResponse);
    }

    /**
     * 发布模板到公共市场
     */
    @Transactional
    public void publishTemplate(Long id) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板无需发布");
        }

        template.setMarketStatus(MarketStatus.PUBLISHED);
        template.setPublishAt(Instant.now());
        gateway.save(template);
    }

    private TemplateResponse toResponse(ProviderTemplate template) {
        int modelCount = template.getModelsConfig() != null ? template.getModelsConfig().size() : 0;
        return TemplateResponse.builder()
            .id(template.getId())
            .templateCode(template.getTemplateCode())
            .templateName(template.getTemplateName())
            .templateType(template.getTemplateType())
            .providerType(template.getProviderType())
            .providerConfig(template.getProviderConfig())
            .modelsConfig(template.getModelsConfig())
            .authorId(template.getAuthorId())
            .authorName(template.getAuthorName())
            .marketStatus(template.getMarketStatus())
            .publishAt(template.getPublishAt())
            .downloadCount(template.getDownloadCount())
            .tags(template.getTags())
            .description(template.getDescription())
            .iconUrl(template.getIconUrl())
            .status(template.getStatus() != null ? template.getStatus().name() : null)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .modelCount(modelCount)
            .build();
    }
}
