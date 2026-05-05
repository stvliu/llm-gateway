package com.codingas.gateway.application.apikey;

import com.codingas.gateway.application.apikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyResponse;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.GatewayApiKey.ApiKeyStatus;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key 应用服务实现
 *
 * <p>处理 API Key 管理的业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyGateway apiKeyGateway;
    private final UserGateway userGateway;
    private final ApiKeyEncryptionDomainService encryptionService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 创建 API Key
     */
    @Override
    @Transactional
    public ApiKeyResponse create(ApiKeyCreateRequest request) {
        // 查找用户
        User user = userGateway.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));

        // 生成 API Key
        String rawKey = generateRawKey();
        String keyHash = encryptionService.hashKey(rawKey);

        // 创建 API Key
        GatewayApiKey apiKey = new GatewayApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setUser(user);
        apiKey.setName(request.getName());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setIpWhitelist(request.getIpWhitelist());
        apiKey.setStatus(ApiKeyStatus.ACTIVE);

        GatewayApiKey savedApiKey = apiKeyGateway.save(apiKey);
        ApiKeyResponse response = toResponse(savedApiKey);
        // 返回原始密钥（仅在创建时返回一次）
        response.setRawKey(rawKey);
        return response;
    }

    /**
     * 根据 ID 获取 API Key
     */
    @Override
    public ApiKeyResponse getById(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        return toResponse(apiKey);
    }

    /**
     * 查询 API Key 列表
     */
    @Override
    public PageResponse<ApiKeyResponse> query(ApiKeyQueryRequest request) {
        List<GatewayApiKey> apiKeys = apiKeyGateway.findAll();

        // 过滤
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String keyword = request.getKeyword().toLowerCase();
            apiKeys = apiKeys.stream()
                .filter(k -> k.getName() != null && k.getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        }

        if (request.getUserId() != null) {
            apiKeys = apiKeys.stream()
                .filter(k -> k.getUser().getId().equals(request.getUserId()))
                .collect(Collectors.toList());
        }

        if (request.getStatus() != null) {
            apiKeys = apiKeys.stream()
                .filter(k -> k.getStatus() == request.getStatus())
                .collect(Collectors.toList());
        }

        // 统计
        long total = apiKeys.size();

        // 分页
        int offset = request.getOffset();
        int limit = request.getLimit();
        List<GatewayApiKey> pagedApiKeys = apiKeys.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        List<ApiKeyResponse> responses = pagedApiKeys.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return PageResponse.of(responses, request.getPage(), limit, total);
    }

    /**
     * 更新 API Key
     */
    @Override
    @Transactional
    public ApiKeyResponse update(Long id, ApiKeyUpdateRequest request) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));

        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }
        if (request.getExpiresAt() != null) {
            apiKey.setExpiresAt(request.getExpiresAt());
        }
        if (request.getIpWhitelist() != null) {
            apiKey.setIpWhitelist(request.getIpWhitelist());
        }
        if (request.getEnabled() != null) {
            apiKey.setStatus(request.getEnabled() ? ApiKeyStatus.ACTIVE : ApiKeyStatus.DISABLED);
        }

        return toResponse(apiKeyGateway.save(apiKey));
    }

    /**
     * 删除 API Key（软删除）
     */
    @Override
    @Transactional
    public void delete(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setDeletedAt(Instant.now());
        apiKeyGateway.save(apiKey);
    }

    /**
     * 启用/禁用 API Key
     */
    @Override
    @Transactional
    public ApiKeyResponse setEnabled(Long id, boolean enabled) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setStatus(enabled ? ApiKeyStatus.ACTIVE : ApiKeyStatus.DISABLED);
        return toResponse(apiKeyGateway.save(apiKey));
    }

    /**
     * 生成原始 API Key
     */
    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "sk-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 转换为响应 DTO
     */
    private ApiKeyResponse toResponse(GatewayApiKey apiKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(apiKey.getId());
        response.setUserId(apiKey.getUser().getId());
        response.setUsername(apiKey.getUser().getUsername());
        response.setName(apiKey.getName());
        response.setStatus(apiKey.getStatus());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setLastUsedAt(apiKey.getLastUsedAt());
        response.setIpWhitelist(apiKey.getIpWhitelist());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setUpdatedAt(apiKey.getUpdatedAt());
        return response;
    }
}
