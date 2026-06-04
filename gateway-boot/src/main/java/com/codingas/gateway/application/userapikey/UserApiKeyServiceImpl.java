package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.iam.service.GeneratedApiKey;
import com.codingas.gateway.domain.iam.service.UserApiKeyGenerator;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.enums.UserApiKeyState;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 API Key 应用服务实现
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserApiKeyServiceImpl.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final UserApiKeyGenerator userApiKeyGenerator;

    public UserApiKeyServiceImpl(UserApiKeyGateway userApiKeyGateway,
                                 UserApiKeyGenerator userApiKeyGenerator) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.userApiKeyGenerator = userApiKeyGenerator;
    }

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        GeneratedApiKey generated = userApiKeyGenerator.generate();

        UserApiKey apiKey = new UserApiKey();
        apiKey.setUserId(request.userId());
        apiKey.setKeyPrefix(generated.keyPrefix());
        apiKey.setKeyPlain(generated.plainKey());
        apiKey.setName(request.name());
        apiKey.setModels(request.models());
        apiKey.setQuotaLimit(request.quotaLimit());
        apiKey.setState(UserApiKeyState.ACTIVE);

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: id={}, userId={}", saved.getId(), saved.getUserId());

        return new UserApiKeyCreateResponse(saved.getId(), generated.keyPrefix(), generated.plainKey());
    }

    @Override
    public List<UserApiKeyResponse> findByUserId(Long userId) {
        return userApiKeyGateway.findByUserId(userId).stream()
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
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
        userApiKeyGateway.delete(apiKey);
        log.info("Deleted UserApiKey: id={}", id);
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getUserId(),
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

    private UserApiKeyDetailResponse toDetailResponse(UserApiKey apiKey) {
        return new UserApiKeyDetailResponse(
                apiKey.getId(),
                apiKey.getUserId(),
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