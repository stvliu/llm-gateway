/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.service;

import com.codingas.gateway.provider.vendor.ConnectivityTestRequest;
import com.codingas.gateway.provider.vendor.ModelNestedRequest;
import com.codingas.gateway.provider.vendor.ProviderCreateRequest;
import com.codingas.gateway.provider.vendor.ProviderQueryRequest;
import com.codingas.gateway.provider.vendor.ProviderResponse;
import com.codingas.gateway.provider.vendor.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.ModelInstanceGateway;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.upstream.ConnectivityTester;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
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
    private final ChannelGateway channelGateway;
    private final ModelInstanceGateway modelInstanceGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
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
        provider.setCode(request.getCode());
        provider.setWebsiteUrl(request.getWebsiteUrl());
        provider.setApiDocUrl(request.getApiDocUrl());
        provider.setPriority(request.getPriority() != null ? request.getPriority() : 100);

        Provider savedProvider = providerGateway.save(provider);
        Long providerId = savedProvider.getId();

        // 创建嵌套的模型
        if (request.getModels() != null && !request.getModels().isEmpty()) {
            for (ModelNestedRequest modelRequest : request.getModels()) {
                Model model = new Model();
                model.setModelName(modelRequest.getModelName());
                model.setDisplayName(modelRequest.getDisplayName());
                model.setContextWindow(modelRequest.getContextWindow());
                model.setCapabilities(modelRequest.getCapabilities());
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
     * <p>同时删除关联的 Product、ProductApiKey 和 Model。</p>
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));

        List<Channel> channels = channelGateway.findByProviderId(id);

        // 1. 删除 ModelInstance（所有状态，包括 INACTIVE）
        for (Channel channel : channels) {
            List<ModelInstance> modelInstances = modelInstanceGateway.findByChannelId(channel.getId());
            for (ModelInstance mi : modelInstances) {
                modelInstanceGateway.deleteById(mi.getId());
            }
        }
        log.info("Deleted model instances for provider {}", id);

        // 3. 删除渠道凭证
        for (Channel channel : channels) {
            List<ChannelCredential> credentials = channelCredentialGateway.findByChannelId(channel.getId());
            for (ChannelCredential credential : credentials) {
                channelCredentialGateway.deleteById(credential.getId());
            }
        }
        log.info("Deleted {} channels' credentials for provider {}", channels.size(), id);

        // 4. 删除渠道
        for (Channel channel : channels) {
            channelGateway.deleteById(channel.getId());
        }
        log.info("Deleted {} channels for provider {}", channels.size(), id);

        // 5. 最后删除 Provider
        providerGateway.delete(provider);
        log.info("Deleted provider {}", id);
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
    public com.codingas.gateway.provider.vendor.ConnectivityTestResult testConnectivity(ConnectivityTestRequest request) {
        ConnectivityTestResult vo = connectivityTester.test(
                request.baseUrl(),
                request.apiKey(),
                request.protocolName()
        );

        // 将 VO 转为应用层 DTO
        return new com.codingas.gateway.provider.vendor.ConnectivityTestResult(
                vo.success(),
                vo.errorMessage() != null ? vo.errorMessage() : "连通性测试成功",
                null,
                new com.codingas.gateway.provider.vendor.ConnectivityTestResult.LevelResult(
                        vo.success(),
                        vo.errorMessage() != null ? vo.errorMessage() : "认证成功",
                        vo.latencyMs(),
                        null,
                        null
                ),
                null,
                vo.latencyMs()
        );
    }

    /**
     * 转换为响应 DTO
     */
    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderId(provider.getCode());
        response.setProviderName(provider.getName());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }
}