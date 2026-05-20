package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * 用户 API Key 应用服务实现
 *
 * <p>加解密由基础设施层（GatewayImpl）处理，Application 层只传递明文 Key。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserApiKeyGateway userApiKeyGateway;

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        // 生成明文 Key
        String plainKey = generateRawKey();
        String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

        UserApiKey apiKey = new UserApiKey();
        apiKey.setTeamId(request.teamId());
        apiKey.setProductId(request.productId());
        apiKey.setKeyPlain(plainKey);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setName(request.name());
        apiKey.setModels(request.models());
        apiKey.setQuotaLimit(request.quotaLimit());
        apiKey.setState(UserApiKeyState.ACTIVE);

        // GatewayImpl 内部处理加密和哈希
        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: id={}, teamId={}", saved.getId(), saved.getTeamId());

        return new UserApiKeyCreateResponse(saved.getId(), keyPrefix, plainKey);
    }

    @Override
    public List<UserApiKeyResponse> listByTeamId(Long teamId) {
        return userApiKeyGateway.findByTeamId(teamId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserApiKeyResponse getById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", id));
        return toResponse(apiKey);
    }

    @Override
    public UserApiKeyDetailResponse getDetailById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", id));
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", id));

        if (request.name() != null) {
            apiKey.setName(request.name());
        }
        if (request.models() != null) {
            apiKey.setModels(request.models());
        }
        if (request.quotaLimit() != null) {
            apiKey.setQuotaLimit(request.quotaLimit());
        }
        if (request.state() != null) {
            apiKey.setState(request.state());
        }

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Updated UserApiKey: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (userApiKeyGateway.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("UserApiKey", id);
        }
        userApiKeyGateway.deleteById(id);
        log.info("Deleted UserApiKey: id={}", id);
    }

    /**
     * 生成原始 API Key（sk- 前缀 + 32字节随机数）
     */
    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "sk-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getTeamId(),
                apiKey.getProductId(),
                apiKey.getKeyPrefix(),
                apiKey.getName(),
                apiKey.getModels(),
                apiKey.getQuotaLimit(),
                apiKey.getState(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
        return new UserApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getTeamId(),
                apiKey.getProductId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getModels(),
                apiKey.getQuotaLimit(),
                apiKey.getState(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }
}