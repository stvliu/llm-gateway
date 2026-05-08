package com.codingas.gateway.infrastructure.template.gateway;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateDo;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provider 模板网关实现
 *
 * <p>实现 ProviderTemplateGateway 接口，负责 DO ↔ Entity 转换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderTemplateGatewayImpl implements ProviderTemplateGateway {

    private final ProviderTemplateRepository repository;

    @Override
    public ProviderTemplate save(ProviderTemplate template) {
        ProviderTemplateDo doEntity = toDo(template);
        ProviderTemplateDo saved = repository.save(doEntity);
        return toEntity(saved);
    }

    @Override
    public Optional<ProviderTemplate> findById(Long id) {
        return repository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<ProviderTemplate> findByTemplateCode(String templateCode) {
        return repository.findByTemplateCode(templateCode).map(this::toEntity);
    }

    @Override
    public Page<ProviderTemplate> findByConditions(
            TemplateType templateType,
            String providerType,
            String keyword,
            MarketStatus marketStatus,
            Pageable pageable) {
        ProviderTemplateDo.TemplateType doTemplateType = templateType != null
            ? ProviderTemplateDo.TemplateType.valueOf(templateType.name())
            : null;
        ProviderTemplateDo.MarketStatus doMarketStatus = marketStatus != null
            ? ProviderTemplateDo.MarketStatus.valueOf(marketStatus.name())
            : null;
        return repository.findByConditions(doTemplateType, providerType, keyword, doMarketStatus, pageable)
            .map(this::toEntity);
    }

    @Override
    public List<ProviderTemplate> findOfficialTemplates() {
        return repository.findOfficialTemplates().stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public Page<ProviderTemplate> findMarketTemplates(Pageable pageable) {
        return repository.findMarketTemplates(pageable).map(this::toEntity);
    }

    @Override
    public List<ProviderTemplate> findByAuthorId(Long authorId) {
        return repository.findByAuthorId(authorId).stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        repository.softDelete(id, Instant.now());
    }

    @Override
    public boolean existsByTemplateCode(String templateCode) {
        return repository.existsByTemplateCode(templateCode);
    }

    @Override
    public void updateMarketStatus(Long id, MarketStatus marketStatus) {
        repository.updateMarketStatus(id, ProviderTemplateDo.MarketStatus.valueOf(marketStatus.name()));
    }

    @Override
    public void incrementDownloadCount(Long id) {
        repository.incrementDownloadCount(id);
    }

    /**
     * DO 转 Entity
     */
    private ProviderTemplate toEntity(ProviderTemplateDo doEntity) {
        if (doEntity == null) {
            return null;
        }
        ProviderTemplate entity = new ProviderTemplate();
        entity.setId(doEntity.getId());
        entity.setTemplateCode(doEntity.getTemplateCode());
        entity.setTemplateName(doEntity.getTemplateName());
        entity.setProviderType(doEntity.getProviderType());
        entity.setProviderConfig(doEntity.getProviderConfig());
        entity.setModelsConfig(doEntity.getModelsConfig());
        entity.setAuthorId(doEntity.getAuthorId());
        entity.setAuthorName(doEntity.getAuthorName());
        entity.setPublishAt(doEntity.getPublishAt());
        entity.setDownloadCount(doEntity.getDownloadCount());
        entity.setTags(doEntity.getTags());
        entity.setDescription(doEntity.getDescription());
        entity.setIconUrl(doEntity.getIconUrl());
        entity.setDeletedAt(doEntity.getDeletedAt());
        entity.setCreatedAt(doEntity.getCreatedAt());
        entity.setCreatedBy(doEntity.getCreatedBy());
        entity.setUpdatedAt(doEntity.getUpdatedAt());
        entity.setUpdatedBy(doEntity.getUpdatedBy());

        // 枚举转换
        if (doEntity.getTemplateType() != null) {
            entity.setTemplateType(TemplateType.valueOf(doEntity.getTemplateType().name()));
        }
        if (doEntity.getMarketStatus() != null) {
            entity.setMarketStatus(MarketStatus.valueOf(doEntity.getMarketStatus().name()));
        }
        if (doEntity.getStatus() != null) {
            entity.setStatus(ProviderTemplate.TemplateStatus.valueOf(doEntity.getStatus().name()));
        }
        return entity;
    }

    /**
     * Entity 转 DO
     */
    private ProviderTemplateDo toDo(ProviderTemplate entity) {
        if (entity == null) {
            return null;
        }
        ProviderTemplateDo doEntity = new ProviderTemplateDo();
        if (entity.getId() != null) {
            doEntity.setId(entity.getId());
        }
        doEntity.setTemplateCode(entity.getTemplateCode());
        doEntity.setTemplateName(entity.getTemplateName());
        doEntity.setProviderType(entity.getProviderType());
        doEntity.setProviderConfig(entity.getProviderConfig());
        doEntity.setModelsConfig(entity.getModelsConfig());
        doEntity.setAuthorId(entity.getAuthorId());
        doEntity.setAuthorName(entity.getAuthorName());
        doEntity.setPublishAt(entity.getPublishAt());
        doEntity.setDownloadCount(entity.getDownloadCount() != null ? entity.getDownloadCount() : 0);
        doEntity.setTags(entity.getTags());
        doEntity.setDescription(entity.getDescription());
        doEntity.setIconUrl(entity.getIconUrl());
        doEntity.setDeletedAt(entity.getDeletedAt());

        // 枚举转换（设置默认值）
        if (entity.getTemplateType() != null) {
            doEntity.setTemplateType(ProviderTemplateDo.TemplateType.valueOf(entity.getTemplateType().name()));
        }
        if (entity.getMarketStatus() != null) {
            doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.valueOf(entity.getMarketStatus().name()));
        } else {
            doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.PRIVATE);
        }
        if (entity.getStatus() != null) {
            doEntity.setStatus(ProviderTemplateDo.TemplateStatus.valueOf(entity.getStatus().name()));
        } else {
            doEntity.setStatus(ProviderTemplateDo.TemplateStatus.ACTIVE);
        }
        return doEntity;
    }
}
