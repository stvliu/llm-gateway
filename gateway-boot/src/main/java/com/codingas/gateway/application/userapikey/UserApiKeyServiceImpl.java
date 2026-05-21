package com.codingas.gateway.application.userapikey;

import com.codingas.gateway.application.userapikey.dto.ProductBrief;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.product.entity.Product;
import com.codingas.gateway.domain.product.gateway.ProductGateway;
import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * 用户 API Key 应用服务实现
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(UserApiKeyServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserApiKeyGateway userApiKeyGateway;
    private final ProductGateway productGateway;

    public UserApiKeyServiceImpl(UserApiKeyGateway userApiKeyGateway,
                                ProductGateway productGateway) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.productGateway = productGateway;
    }

    @Override
    @Transactional
    public UserApiKeyCreateResponse create(UserApiKeyCreateRequest request) {
        String plainKey = generateRawKey();
        String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

        UserApiKey apiKey = new UserApiKey();
        apiKey.setTeamId(request.teamId());
        apiKey.setUserId(request.userId());
        apiKey.setProductIds(request.productIds());
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setKeyPlain(plainKey);
        apiKey.setName(request.name());
        apiKey.setModels(request.models());
        apiKey.setQuotaLimit(request.quotaLimit());
        apiKey.setState(UserApiKeyState.ACTIVE);

        UserApiKey saved = userApiKeyGateway.save(apiKey);
        log.info("Created UserApiKey: id={}, teamId={}, userId={}, productIds={}",
                saved.getId(), saved.getTeamId(), saved.getUserId(), saved.getProductIds());

        return new UserApiKeyCreateResponse(saved.getId(), keyPrefix, plainKey);
    }

    @Override
    public List<UserApiKeyResponse> listByTeamId(Long teamId) {
        return userApiKeyGateway.findByTeamId(teamId).stream()
                .map(this::toResponse)
                .toList();
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
                .orElseThrow(() -> new IllegalArgumentException("UserApiKey not found: id=" + id));
        return toResponse(apiKey);
    }

    @Override
    public UserApiKeyDetailResponse getDetailById(Long id) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UserApiKey not found: id=" + id));
        return toDetailResponse(apiKey);
    }

    @Override
    @Transactional
    public UserApiKeyResponse update(Long id, UserApiKeyUpdateRequest request) {
        UserApiKey apiKey = userApiKeyGateway.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UserApiKey not found: id=" + id));

        if (request.name() != null) {
            apiKey.setName(request.name());
        }
        if (request.productIds() != null) {
            apiKey.setProductIds(request.productIds());
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
        userApiKeyGateway.deleteById(id);
        log.info("Deleted UserApiKey: id={}", id);
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "sk-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserApiKeyResponse toResponse(UserApiKey apiKey) {
        return new UserApiKeyResponse(
                apiKey.getId(),
                apiKey.getTeamId(),
                apiKey.getUserId(),
                apiKey.getProductIds(),
                toProductBriefs(apiKey.getProductIds()),
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
                apiKey.getUserId(),
                apiKey.getProductIds(),
                toProductBriefs(apiKey.getProductIds()),
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

    /** 将 productIds 转为 ProductBrief 列表（批量查询避免 N+1） */
    private List<ProductBrief> toProductBriefs(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productGateway.findByIds(productIds).stream()
                .map(p -> new ProductBrief(p.getId(), p.getName()))
                .toList();
    }
}