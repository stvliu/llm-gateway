package com.codingas.gateway.application.gatewayapikey;

import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUsageResponse;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import com.codingas.gateway.domain.security.entity.GatewayApiKey;
import com.codingas.gateway.domain.security.entity.User;
import com.codingas.gateway.domain.security.gateway.ApiKeyGateway;
import com.codingas.gateway.domain.security.gateway.UserGateway;
import com.codingas.gateway.domain.security.service.ApiKeyEncryptionDomainService;
import com.codingas.gateway.infrastructure.audit.gateway.database.UsageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final UsageLogRepository usageLogRepository;

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
        String keyEncrypted = encryptionService.encrypt(rawKey);

        // 创建 API Key
        GatewayApiKey apiKey = new GatewayApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyEncrypted(keyEncrypted);
        apiKey.setUserId(user.getId());
        apiKey.setUsername(user.getUsername());
        apiKey.setName(request.getName());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setIpWhitelist(request.getIpWhitelist());
        apiKey.setState(GatewayApiKeyState.ACTIVE);

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
                .filter(k -> k.getUserId().equals(request.getUserId()))
                .collect(Collectors.toList());
        }

        if (request.getState() != null) {
            apiKeys = apiKeys.stream()
                .filter(k -> k.getState() == request.getState())
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
        if (request.getState() != null) {
            apiKey.setState(request.getState());
        }

        return toResponse(apiKeyGateway.save(apiKey));
    }

    /**
     * 删除 API Key
     */
    @Override
    @Transactional
    public void delete(Long id) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKeyGateway.delete(apiKey);
    }

    /**
     * 启用/禁用 API Key
     */
    @Override
    @Transactional
    public ApiKeyResponse setEnabled(Long id, boolean enabled) {
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));
        apiKey.setState(enabled ? GatewayApiKeyState.ACTIVE : GatewayApiKeyState.DISABLED);
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
        response.setUserId(apiKey.getUserId());
        response.setUsername(apiKey.getUsername());
        response.setName(apiKey.getName());
        // 解密返回完整 Key
        if (apiKey.getKeyEncrypted() != null && !apiKey.getKeyEncrypted().isBlank()) {
            try {
                response.setKey(encryptionService.decrypt(apiKey.getKeyEncrypted()));
            } catch (Exception e) {
                log.warn("Failed to decrypt API Key: {}", e.getMessage());
                // 解密失败时显示脱敏标识
                if (apiKey.getKeyHash() != null && apiKey.getKeyHash().length() >= 8) {
                    response.setKey("sk-****" + apiKey.getKeyHash().substring(0, 8));
                }
            }
        } else if (apiKey.getKeyHash() != null && apiKey.getKeyHash().length() >= 8) {
            // 旧数据没有加密 Key 时显示脱敏标识
            response.setKey("sk-****" + apiKey.getKeyHash().substring(0, 8));
        }
        response.setState(apiKey.getState());
        response.setExpiresAt(apiKey.getExpiresAt());
        response.setLastUsedAt(apiKey.getLastUsedAt());
        response.setIpWhitelist(apiKey.getIpWhitelist());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setUpdatedAt(apiKey.getUpdatedAt());
        return response;
    }

    /**
     * 获取单个 API Key 的用量统计
     */
    @Override
    public ApiKeyUsageResponse getUsage(Long id, Instant startDate, Instant endDate) {
        // 验证 API Key 存在
        GatewayApiKey apiKey = apiKeyGateway.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ApiKey", id));

        // 默认时间范围：最近30天
        Instant start = startDate != null ? startDate : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant end = endDate != null ? endDate : Instant.now();

        // 查询用量统计
        List<Object[]> results = usageLogRepository.aggregateByApiKeyId(id, start, end);

        if (results.isEmpty() || results.get(0) == null) {
            return ApiKeyUsageResponse.builder()
                .apiKeyId(id)
                .apiKeyName(apiKey.getName())
                .totalCalls(0)
                .totalInputTokens(0)
                .totalOutputTokens(0)
                .totalTokens(0)
                .startDate(start)
                .endDate(end)
                .build();
        }

        Object[] row = results.get(0);
        return ApiKeyUsageResponse.builder()
            .apiKeyId(id)
            .apiKeyName(apiKey.getName())
            .totalCalls(((Number) row[0]).longValue())
            .totalInputTokens(((Number) row[1]).longValue())
            .totalOutputTokens(((Number) row[2]).longValue())
            .totalTokens(((Number) row[3]).longValue())
            .startDate(start)
            .endDate(end)
            .build();
    }

    /**
     * 批量获取 API Key 的用量统计
     */
    @Override
    public List<ApiKeyUsageResponse> getUsageBatch(Instant startDate, Instant endDate, Long userId) {
        // 默认时间范围：最近30天
        Instant start = startDate != null ? startDate : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant end = endDate != null ? endDate : Instant.now();

        // 获取 API Key 列表（按 userId 过滤）
        List<GatewayApiKey> apiKeys;
        if (userId != null) {
            apiKeys = apiKeyGateway.findByUserId(userId);
        } else {
            apiKeys = apiKeyGateway.findAll();
        }

        if (apiKeys.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 ID -> Name 映射
        Map<Long, String> apiKeyNameMap = apiKeys.stream()
            .collect(Collectors.toMap(GatewayApiKey::getId, GatewayApiKey::getName));

        // 批量查询用量统计
        List<Long> apiKeyIds = apiKeys.stream()
            .map(GatewayApiKey::getId)
            .collect(Collectors.toList());

        List<Object[]> results = usageLogRepository.aggregateByApiKeyIds(apiKeyIds, start, end);

        // 构建 API Key ID -> 统计结果映射
        Map<Long, Object[]> usageMap = results.stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                Function.identity()
            ));

        // 构建响应列表
        return apiKeys.stream()
            .map(apiKey -> {
                Object[] usage = usageMap.get(apiKey.getId());
                if (usage == null) {
                    return ApiKeyUsageResponse.builder()
                        .apiKeyId(apiKey.getId())
                        .apiKeyName(apiKey.getName())
                        .totalCalls(0)
                        .totalInputTokens(0)
                        .totalOutputTokens(0)
                        .totalTokens(0)
                        .startDate(start)
                        .endDate(end)
                        .build();
                }
                return ApiKeyUsageResponse.builder()
                    .apiKeyId(apiKey.getId())
                    .apiKeyName(apiKey.getName())
                    .totalCalls(((Number) usage[1]).longValue())
                    .totalInputTokens(((Number) usage[2]).longValue())
                    .totalOutputTokens(((Number) usage[3]).longValue())
                    .totalTokens(((Number) usage[4]).longValue())
                    .startDate(start)
                    .endDate(end)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
