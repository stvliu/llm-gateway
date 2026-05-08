package com.codingas.gateway.application.template;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.codingas.gateway.infrastructure.template.repository.GitTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 官方模板同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialTemplateSyncService {

    private final GitTemplateRepository gitRepository;
    private final ProviderTemplateGateway gateway;

    /**
     * 同步官方模板
     */
    @Transactional
    public SyncResult syncTemplates() {
        try {
            gitRepository.syncRepository();
            List<Map<String, Object>> templates = gitRepository.loadTemplates();

            int addedCount = 0;
            int updatedCount = 0;

            for (Map<String, Object> templateData : templates) {
                String templateCode = (String) templateData.get("template_code");
                if (templateCode == null) {
                    continue;
                }

                ProviderTemplate template = gateway.findByTemplateCode(templateCode)
                    .orElseGet(() -> {
                        ProviderTemplate t = new ProviderTemplate();
                        t.setTemplateCode(templateCode);
                        t.setTemplateType(TemplateType.OFFICIAL);
                        t.setMarketStatus(MarketStatus.PUBLISHED);
                        t.setDownloadCount(0);
                        return t;
                    });

                updateTemplateFromMap(template, templateData);
                gateway.save(template);

                if (template.getId() == null) {
                    addedCount++;
                } else {
                    updatedCount++;
                }
            }

            log.info("Template sync completed: {} added, {} updated", addedCount, updatedCount);
            return new SyncResult(templates.size(), addedCount, updatedCount, Instant.now());

        } catch (Exception e) {
            log.error("Failed to sync templates", e);
            throw new RuntimeException("模板同步失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateTemplateFromMap(ProviderTemplate template, Map<String, Object> data) {
        template.setTemplateName((String) data.get("template_name"));
        template.setProviderType((String) data.get("provider_type"));
        template.setProviderConfig((Map<String, Object>) data.get("provider_config"));
        template.setModelsConfig((List<Map<String, Object>>) data.get("models_config"));
        template.setDescription((String) data.get("description"));
        template.setIconUrl((String) data.get("icon_url"));
        template.setTags((List<String>) data.get("tags"));
    }

    /**
     * 同步结果
     */
    public record SyncResult(int syncedCount, int addedCount, int updatedCount, Instant syncedAt) {}
}