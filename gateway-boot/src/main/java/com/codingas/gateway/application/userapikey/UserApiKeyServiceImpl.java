package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.application.gateway.ApplicationGateway;
import com.codingas.gateway.domain.iam.service.GeneratedApiKey;
import com.codingas.gateway.domain.iam.service.UserApiKeyGenerator;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 API Key 应用服务实现
 *
 * <p>applicationId 为权限锚点：create 必填并校验 Application 存在；
 * update 支持补绑/转移（非 null 时校验存在）。</p>
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserApiKeyServiceImpl.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final UserApiKeyGenerator userApiKeyGenerator;
    private final ApplicationGateway applicationGateway;

    public UserApiKeyServiceImpl(UserApiKeyGateway userApiKeyGateway,
                                 UserApiKeyGenerator userApiKeyGenerator,
                                 ApplicationGateway applicationGateway) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.userApiKeyGenerator = userApiKeyGenerator;
        this.applicationGateway = applicationGateway;
    }

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        // 校验 Application 存在（applicationId 为权限锚点，引用必须有效）
        validateApplicationExists(request.applicationId());

        GeneratedApiKey generated = userApiKeyGenerator.generate();

        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(request.userId());
        apiKey.setApplicationId(request.applicationId());
        apiKey.setKeyPrefix(generated.keyPrefix());
        apiKey.setKeyPlain(generated.plainKey());
        apiKey.setName(request.name());

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: id={}, userId={}, applicationId={}",
                saved.getId(), saved.getUserId(), saved.getApplicationId());

        return new UserApiKeyCreateResponse(saved.getId(), generated.keyPrefix(), generated.plainKey());
    }

    @Override
    public List<UserApiKeyResponse> findByUserId(Long userId) {
        return userApiKeyGateway.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findAllNonDeleted() {
        return userApiKeyGateway.findAllNonDeleted().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<UserApiKeyResponse> findByApplicationId(Long applicationId) {
        return userApiKeyGateway.findByApplicationId(applicationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public UserApiKeyResponse getById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        return toResponse(apiKey);
    }

    @Override
    public UserApiKeyDetailResponse getDetailById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));

        // 补绑/转移 applicationId（非 null 时校验存在）
        if (request.applicationId() != null) {
            validateApplicationExists(request.applicationId());
            apiKey.setApplicationId(request.applicationId());
        }
        if (request.name() != null) {
            apiKey.setName(request.name());
        }

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Updated UserApiKey: id={}, applicationId={}", saved.getId(), saved.getApplicationId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        userApiKeyGateway.delete(apiKey);
        log.info("Deleted UserApiKey: id={}", id);
    }

    /**
     * 校验 Application 存在
     *
     * <p>ApplicationGateway.findById 返回 null 表示不存在（沿用现有约定）。</p>
     *
     * @param applicationId 应用 ID
     * @throws GatewayRequestException 应用不存在时抛 APPLICATION_NOT_FOUND
     */
    private void validateApplicationExists(Long applicationId) {
        if (applicationGateway.findById(applicationId) == null) {
            throw new GatewayRequestException("APPLICATION_NOT_FOUND", "应用不存在: " + applicationId);
        }
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }

    private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
        return new UserApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getUserId(),
                apiKey.getApplicationId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain(),
                apiKey.getName(),
                apiKey.getCreatedAt(),
                apiKey.getUpdatedAt()
        );
    }
}
