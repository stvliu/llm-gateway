package com.codingas.gateway.application.template;

import com.codingas.gateway.application.template.dto.*;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.gateway.*;
import com.codingas.gateway.domain.template.entity.MarketState;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Provider 模板应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTemplateService {

    private final ProviderTemplateGateway gateway;
    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ObjectMapper objectMapper;

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
        template.setMarketState(MarketState.PRIVATE);
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
            MarketState marketState,
            int page,
            int limit) {

        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<ProviderTemplate> result = gateway.findByConditions(type, providerType, keyword, marketState, pageable);
        return result.map(this::toResponse);
    }

    /**
     * 更新模板市场状态
     */
    @Transactional
    public TemplateResponse updateMarketState(Long id, MarketState marketState) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        if (TemplateType.OFFICIAL.equals(template.getTemplateType())) {
            throw new IllegalStateException("官方模板状态不允许修改");
        }

        template.setMarketState(marketState);
        if (marketState == MarketState.PUBLISHED) {
            template.setPublishAt(Instant.now());
        }
        ProviderTemplate saved = gateway.save(template);
        return toResponse(saved);
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

        template.setMarketState(MarketState.PUBLISHED);
        template.setPublishAt(Instant.now());
        gateway.save(template);
    }

    /**
     * 应用模板创建 Provider、Model、ApiKey
     */
    @Transactional
    public ApplyTemplateResult applyTemplate(Long templateId, ApplyTemplateRequest request, Long userId) {
        ProviderTemplate template = gateway.findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        // 1. 创建 Provider
        Provider provider = new Provider();
        provider.setName(template.getTemplateName());
        provider.setType(ProviderType.valueOf(template.getProviderType()));
        Map<String, Object> providerConfig = template.getProviderConfig();
        if (providerConfig != null) {
            if (providerConfig.containsKey("base_url")) {
                provider.setBaseUrl((String) providerConfig.get("base_url"));
            }
            if (providerConfig.containsKey("website_url")) {
                provider.setWebsiteUrl((String) providerConfig.get("website_url"));
            }
            if (providerConfig.containsKey("api_doc_url")) {
                provider.setApiDocUrl((String) providerConfig.get("api_doc_url"));
            }
            if (providerConfig.containsKey("timeout")) {
                provider.setTimeout(((Number) providerConfig.get("timeout")).intValue());
            }
            if (providerConfig.containsKey("max_retries")) {
                provider.setMaxRetries(((Number) providerConfig.get("max_retries")).intValue());
            }
        }
        provider.setPriority(100);
        provider.setState(ProviderState.ACTIVE);
        Provider savedProvider = providerGateway.save(provider);

        // 2. 创建 Model
        List<Long> modelIds = new ArrayList<>();
        List<String> modelNames = new ArrayList<>();
        List<Map<String, Object>> modelsConfig = template.getModelsConfig();
        if (modelsConfig != null) {
            for (Map<String, Object> modelConfig : modelsConfig) {
                Model model = new Model();
                model.setProviderId(savedProvider.getId());
                model.setProviderName(template.getTemplateName());
                model.setProviderModelId((String) modelConfig.get("provider_model_id"));
                model.setDisplayName((String) modelConfig.get("display_name"));
                if (modelConfig.containsKey("context_window")) {
                    model.setContextWindow(((Number) modelConfig.get("context_window")).intValue());
                }
                if (modelConfig.containsKey("input_price")) {
                    Object inputPrice = modelConfig.get("input_price");
                    if (inputPrice instanceof Number) {
                        model.setInputPrice(BigDecimal.valueOf(((Number) inputPrice).doubleValue()));
                    }
                }
                if (modelConfig.containsKey("output_price")) {
                    Object outputPrice = modelConfig.get("output_price");
                    if (outputPrice instanceof Number) {
                        model.setOutputPrice(BigDecimal.valueOf(((Number) outputPrice).doubleValue()));
                    }
                }
                if (modelConfig.containsKey("capabilities")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> caps = (Map<String, Boolean>) modelConfig.get("capabilities");
                    model.setCapabilities(caps);
                }
                model.setState(ModelState.ACTIVE);
                Model savedModel = modelGateway.save(model);
                modelIds.add(savedModel.getId());
                modelNames.add(savedModel.getDisplayName());
            }
        }

        // 3. 创建 ApiKey（Gateway 层加密存储）
        ProviderApiKey apiKey = new ProviderApiKey();
        apiKey.setProviderId(savedProvider.getId());
        apiKey.setKeyName(template.getTemplateName() + " API Key");
        apiKey.setApiKey(request.getApiKey());
        apiKey.setPriority(100);
        apiKey.setState(ProviderApiKeyState.ACTIVE);
        providerApiKeyGateway.save(apiKey);

        // 4. 增加模板使用次数
        gateway.incrementDownloadCount(templateId);

        return ApplyTemplateResult.builder()
            .providerId(savedProvider.getId())
            .providerName(savedProvider.getName())
            .modelIds(modelIds)
            .modelNames(modelNames)
            .createdAt(savedProvider.getCreatedAt())
            .build();
    }

    /**
     * 导出模板为 JSON 文件
     */
    public void exportTemplate(Long id, OutputStream out) {
        ProviderTemplate template = gateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry(template.getTemplateCode() + ".json");
            zipOut.putNextEntry(entry);

            TemplateExportDto exportDto = toExportDto(template);
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDto);
            zipOut.write(json.getBytes(StandardCharsets.UTF_8));

            zipOut.closeEntry();
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量导出模板为 ZIP 文件
     */
    public void exportTemplates(List<Long> ids, OutputStream out) {
        try (ZipOutputStream zipOut = new ZipOutputStream(out)) {
            for (Long id : ids) {
                ProviderTemplate template = gateway.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

                ZipEntry entry = new ZipEntry(template.getTemplateCode() + ".json");
                zipOut.putNextEntry(entry);

                TemplateExportDto exportDto = toExportDto(template);
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDto);
                zipOut.write(json.getBytes(StandardCharsets.UTF_8));

                zipOut.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 ZIP 文件导入模板
     */
    @Transactional
    public List<TemplateResponse> importTemplates(InputStream in, Long userId, String username) {
        List<TemplateResponse> results = new ArrayList<>();

        try (ZipInputStream zipIn = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().endsWith(".json")) {
                    continue;
                }

                String json = new String(zipIn.readAllBytes(), StandardCharsets.UTF_8);
                TemplateExportDto exportDto = objectMapper.readValue(json, TemplateExportDto.class);

                // 转换为创建请求
                TemplateCreateRequest request = new TemplateCreateRequest();
                request.setTemplateCode(generateImportCode(exportDto.getTemplateCode()));
                request.setTemplateName(exportDto.getTemplateName());
                request.setProviderType(exportDto.getProviderType());
                request.setProviderConfig(exportDto.getProviderConfig());
                request.setModelsConfig(exportDto.getModelsConfig());
                request.setDescription(exportDto.getDescription());
                request.setIconUrl(exportDto.getIconUrl());
                request.setTags(exportDto.getTags());

                TemplateResponse response = createTemplate(request, userId, username);
                results.add(response);
            }
        } catch (Exception e) {
            throw new RuntimeException("导入模板失败: " + e.getMessage(), e);
        }

        return results;
    }

    /**
     * 生成导入时的模板编码（避免冲突）
     */
    private String generateImportCode(String originalCode) {
        String baseCode = originalCode + "_import";
        if (!gateway.existsByTemplateCode(baseCode)) {
            return baseCode;
        }
        return baseCode + "_" + System.currentTimeMillis();
    }

    /**
     * 转换为导出 DTO
     */
    private TemplateExportDto toExportDto(ProviderTemplate template) {
        return TemplateExportDto.builder()
            .templateCode(template.getTemplateCode())
            .templateName(template.getTemplateName())
            .providerType(template.getProviderType())
            .providerConfig(template.getProviderConfig())
            .modelsConfig(template.getModelsConfig())
            .description(template.getDescription())
            .iconUrl(template.getIconUrl())
            .tags(template.getTags())
            .build();
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
            .marketState(template.getMarketState())
            .publishAt(template.getPublishAt())
            .downloadCount(template.getDownloadCount())
            .tags(template.getTags())
            .description(template.getDescription())
            .iconUrl(template.getIconUrl())
            .state(template.getState() != null ? template.getState().name() : null)
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .modelCount(modelCount)
            .build();
    }
}