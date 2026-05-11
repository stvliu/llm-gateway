package com.codingas.gateway.application.provider;

import com.codingas.gateway.application.provider.dto.ModelNestedRequest;
import com.codingas.gateway.application.provider.dto.ProviderApiKeyNestedRequest;
import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderKeyStats;
import com.codingas.gateway.application.provider.dto.ProviderKeysResponse;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提供商应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderGateway providerGateway;
    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ModelGateway modelGateway;

    /**
     * 创建提供商
     */
    @Override
    @Transactional
    public ProviderResponse create(ProviderCreateRequest request) {
        Provider provider = new Provider();
        provider.setName(request.getProviderName());
        provider.setType(request.getProviderType());
        provider.setBaseUrl(request.getBaseUrl());
        provider.setWebsiteUrl(request.getWebsiteUrl());
        provider.setApiDocUrl(request.getApiDocUrl());
        provider.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        provider.setState(ProviderState.ACTIVE);

        Provider savedProvider = providerGateway.save(provider);
        Long providerId = savedProvider.getId();

        // 创建嵌套的 API Keys
        if (request.getApiKeys() != null && !request.getApiKeys().isEmpty()) {
            for (ProviderApiKeyNestedRequest keyRequest : request.getApiKeys()) {
                ProviderApiKey apiKey = new ProviderApiKey();
                apiKey.setProviderId(providerId);
                apiKey.setKeyName(keyRequest.getKeyName());
                apiKey.setApiKey(keyRequest.getApiKey());
                apiKey.setPriority(keyRequest.getPriority() != null ? keyRequest.getPriority() : 100);
                apiKey.setWeight(keyRequest.getWeight() != null ? keyRequest.getWeight() : 100);
                apiKey.setIsDefault(keyRequest.getIsDefault() != null ? keyRequest.getIsDefault() : false);
                apiKey.setState(ProviderApiKeyState.ACTIVE);
                providerApiKeyGateway.save(apiKey);
            }
            log.info("Created {} API keys for provider {}", request.getApiKeys().size(), providerId);
        }

        // 创建嵌套的模型
        if (request.getModels() != null && !request.getModels().isEmpty()) {
            for (ModelNestedRequest modelRequest : request.getModels()) {
                Model model = new Model();
                model.setProviderId(providerId);
                model.setProviderName(savedProvider.getName());
                model.setProviderModelId(modelRequest.getProviderModelId());
                model.setDisplayName(modelRequest.getDisplayName());
                model.setContextWindow(modelRequest.getContextWindow());
                model.setInputPrice(modelRequest.getInputPrice());
                model.setOutputPrice(modelRequest.getOutputPrice());
                model.setCapabilities(modelRequest.getCapabilities());
                model.setState(ModelState.ACTIVE);
                modelGateway.save(model);
            }
            log.info("Created {} models for provider {}", request.getModels().size(), providerId);
        }

        return toResponse(savedProvider);
    }

    /**
     * 根据 ID 获取提供商
     */
    @Override
    public ProviderResponse getById(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        return toResponse(provider);
    }

    /**
     * 查询提供商列表
     */
    @Override
    public PageResponse<ProviderResponse> query(ProviderQueryRequest request) {
        List<Provider> providers = providerGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            providers = providers.stream()
                .filter(p -> p.getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getProviderType() != null) {
            providers = providers.stream()
                .filter(p -> p.getType() == request.getProviderType())
                .collect(Collectors.toList());
        }

        if (request.getState() != null) {
            providers = providers.stream()
                .filter(p -> p.getState().equals(request.getState()))
                .collect(Collectors.toList());
        }

        // 统计
        long total = providers.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<Provider> pagedProviders = providers.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<ProviderResponse> responses = pagedProviders.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        // 批量填充 Key 统计信息
        List<Long> providerIds = pagedProviders.stream()
            .map(Provider::getId)
            .collect(Collectors.toList());
        Map<Long, ProviderKeyStats> keyStatsMap = providerApiKeyGateway.getKeyStatsByProviderIds(providerIds);
        responses.forEach(r -> r.setKeyStats(keyStatsMap.get(r.getId())));

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新提供商
     */
    @Override
    @Transactional
    public ProviderResponse update(Long id, ProviderUpdateRequest request) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));

        if (request.getProviderName() != null) {
            provider.setName(request.getProviderName());
        }
        if (request.getProviderType() != null) {
            provider.setType(request.getProviderType());
        }
        if (request.getBaseUrl() != null) {
            provider.setBaseUrl(request.getBaseUrl());
        }
        if (request.getWebsiteUrl() != null) {
            provider.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getApiDocUrl() != null) {
            provider.setApiDocUrl(request.getApiDocUrl());
        }
        if (request.getPriority() != null) {
            provider.setPriority(request.getPriority());
        }
        if (request.getState() != null) {
            provider.setState(request.getState());
        }

        return toResponse(providerGateway.save(provider));
    }

    /**
     * 删除提供商
     * 先删除关联的 API Keys 和 Models，再删除 Provider
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));

        // 先删除关联的 API Keys
        List<ProviderApiKey> keys = providerApiKeyGateway.findByProviderId(id);
        for (ProviderApiKey key : keys) {
            providerApiKeyGateway.delete(key);
        }
        log.info("Deleted {} API keys for provider {}", keys.size(), id);

        // 再删除关联的 Models
        List<Model> models = modelGateway.findByProviderId(id);
        for (Model model : models) {
            modelGateway.delete(model);
        }
        log.info("Deleted {} models for provider {}", models.size(), id);

        // 最后删除 Provider
        providerGateway.delete(provider);
        log.info("Deleted provider {}", id);
    }

    /**
     * 启用/禁用提供商
     */
    @Override
    @Transactional
    public ProviderResponse setEnabled(Long id, boolean enabled) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        provider.setState(enabled ? ProviderState.ACTIVE : ProviderState.DISABLED);
        return toResponse(providerGateway.save(provider));
    }

    /**
     * 获取 Provider 的 Key 信息（默认 Key + 列表）
     */
    @Override
    public ProviderKeysResponse getProviderKeys(Long providerId) {
        // 验证 Provider 存在
        providerGateway.findById(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", providerId));

        // 获取所有 Key
        List<ProviderApiKey> keys = providerApiKeyGateway.findByProviderId(providerId);
        List<ProviderApiKeyResponse> keyResponses = keys.stream()
            .map(ProviderApiKeyResponse::from)
            .collect(Collectors.toList());

        // 获取默认 Key
        ProviderApiKey defaultKey = providerApiKeyGateway.findDefaultKeyByProviderId(providerId).orElse(null);
        ProviderApiKeyResponse defaultKeyResponse = ProviderApiKeyResponse.from(defaultKey);

        return new ProviderKeysResponse(defaultKeyResponse, keyResponses);
    }

    /**
     * 转换为响应 DTO
     */
    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderName(provider.getName());
        response.setProviderType(provider.getType() != null ? provider.getType() : null);
        response.setBaseUrl(provider.getBaseUrl());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setState(provider.getState());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }
}