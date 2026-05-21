package com.codingas.gateway.application.provider;

import com.codingas.gateway.application.provider.dto.ConnectivityTestRequest;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.application.provider.dto.ModelNestedRequest;
import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.model.enums.ModelState;
import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.model.entity.Provider;
import com.codingas.gateway.domain.model.gateway.ConnectivityTester;
import com.codingas.gateway.domain.model.gateway.ModelGateway;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提供商应用服务实现
 *
 * <p>已移除 ProviderApiKey 相关逻辑，API Key 管理迁移到 ProductApiKey。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderGateway providerGateway;
    private final ModelGateway modelGateway;
    private final ConnectivityTester connectivityTester;

    /**
     * 创建提供商
     *
     * <p>注意：API Key 管理已迁移到 ProductApiKey，创建 Provider 时不再创建 API Key。</p>
     */
    @Override
    @Transactional
    public ProviderResponse create(ProviderCreateRequest request) {
        Provider provider = new Provider();
        provider.setName(request.getProviderName());
        provider.setWebsiteUrl(request.getWebsiteUrl());
        provider.setApiDocUrl(request.getApiDocUrl());
        provider.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        provider.setState(ProviderState.ACTIVE);

        Provider savedProvider = providerGateway.save(provider);
        Long providerId = savedProvider.getId();

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

        if (request.getState() != null && !request.getState().isBlank()) {
            ProviderState state = ProviderState.valueOf(request.getState());
            providers = providers.stream()
                .filter(p -> p.getState().equals(state))
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
        if (request.getWebsiteUrl() != null) {
            provider.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getApiDocUrl() != null) {
            provider.setApiDocUrl(request.getApiDocUrl());
        }
        if (request.getPriority() != null) {
            provider.setPriority(request.getPriority());
        }

        return toResponse(providerGateway.save(provider));
    }

    /**
     * 删除提供商
     *
     * <p>注意：API Key 管理已迁移到 ProductApiKey，删除 Provider 时不再删除 API Key。</p>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));

        // 删除关联的 Models
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
     * 获取所有供应商名称列表
     */
    @Override
    public List<String> getProviderNames() {
        return providerGateway.findAll().stream()
            .map(Provider::getName)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 测试连通性
     */
    @Override
    public ConnectivityTestResult testConnectivity(ConnectivityTestRequest request) {
        return connectivityTester.test(request);
    }

    /**
     * 转换为响应 DTO
     */
    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderName(provider.getName());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setState(provider.getState() != null ? provider.getState().name() : null);
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }
}