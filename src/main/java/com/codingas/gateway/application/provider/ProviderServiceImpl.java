package com.codingas.gateway.application.provider;

import com.codingas.gateway.adapter.admin.dto.provider.ProviderCreateRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderQueryRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderResponse;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.entity.Provider.ProviderStatus;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 提供商应用服务实现
 *
 * <p>处理提供商管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderServiceImpl implements ProviderService {

    private final ProviderGateway providerGateway;

    /**
     * 创建提供商
     */
    @Override
    @Transactional
    public ProviderResponse create(ProviderCreateRequest request) {
        // 检查提供商代码唯一性
        if (providerGateway.existsByProviderCode(request.getProviderCode())) {
            throw new DuplicateResourceException("Provider", "providerCode");
        }

        // 创建提供商
        Provider provider = new Provider();
        provider.setProviderCode(request.getProviderCode());
        provider.setProviderName(request.getProviderName());
        provider.setProviderType(request.getProviderType());
        provider.setBaseUrl(request.getBaseUrl());
        provider.setWebsiteUrl(request.getWebsiteUrl());
        provider.setApiDocUrl(request.getApiDocUrl());
        provider.setPriority(request.getPriority() != null ? request.getPriority() : 100);
        provider.setStatus(ProviderStatus.ACTIVE);

        Provider savedProvider = providerGateway.save(provider);
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
                .filter(p -> p.getProviderCode().toLowerCase().contains(keyword)
                    || p.getProviderName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getProviderType() != null) {
            providers = providers.stream()
                .filter(p -> p.getProviderType() == request.getProviderType())
                .collect(Collectors.toList());
        }

        if (request.getStatus() != null) {
            providers = providers.stream()
                .filter(p -> p.getStatus() == request.getStatus())
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
            provider.setProviderName(request.getProviderName());
        }
        if (request.getProviderType() != null) {
            provider.setProviderType(request.getProviderType());
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
        if (request.getEnabled() != null) {
            // 根据 enabled 状态设置 ProviderStatus
            provider.setStatus(request.getEnabled() ? ProviderStatus.ACTIVE : ProviderStatus.SUSPENDED);
        }

        return toResponse(providerGateway.save(provider));
    }

    /**
     * 删除提供商（软删除）
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        provider.setDeletedAt(Instant.now());
        providerGateway.save(provider);
    }

    /**
     * 启用/禁用提供商
     */
    @Override
    @Transactional
    public ProviderResponse setEnabled(Long id, boolean enabled) {
        Provider provider = providerGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Provider", id));
        provider.setStatus(enabled ? ProviderStatus.ACTIVE : ProviderStatus.SUSPENDED);
        return toResponse(providerGateway.save(provider));
    }

    /**
     * 转换为响应 DTO
     */
    private ProviderResponse toResponse(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderCode(provider.getProviderCode());
        response.setProviderName(provider.getProviderName());
        response.setProviderType(provider.getProviderType());
        response.setBaseUrl(provider.getBaseUrl());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setStatus(provider.getStatus());
        response.setEnabled(provider.getStatus() == ProviderStatus.ACTIVE);
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }
}