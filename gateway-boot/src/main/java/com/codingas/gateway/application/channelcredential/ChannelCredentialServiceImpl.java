package com.codingas.gateway.application.channelcredential;

import com.codingas.gateway.application.channel.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 渠道凭证应用服务实现
 *
 * <p>加解密由基础设施层（GatewayImpl）处理，Application 层只传递明文 Key。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelCredentialServiceImpl implements ChannelCredentialService {

    private final ChannelCredentialGateway channelCredentialGateway;

    @Override
    @Transactional
    public ChannelCredentialCreateResponse create(ChannelCredentialCreateRequest request) {
        String plainKey = request.apiKey();
        String keyPrefix = plainKey.substring(0, Math.min(8, plainKey.length()));

        ChannelCredential credential = new ChannelCredential();
        credential.setChannelId(request.channelId());
        credential.setApiKeyPlain(plainKey);
        credential.setApiKeyPrefix(keyPrefix);
        credential.setName(request.description());
        credential.setWeight(request.weight());
        credential.setPriority(request.priority());
        credential.setState(CredentialState.ACTIVE);

        // GatewayImpl 内部处理加密和哈希
        ChannelCredential saved = channelCredentialGateway.save(credential);
        log.info("Created ChannelCredential: id={}, channelId={}", saved.getId(), saved.getChannelId());

        return new ChannelCredentialCreateResponse(saved.getId(), plainKey);
    }

    @Override
    public List<ChannelCredentialResponse> listByChannelId(Long channelId) {
        return channelCredentialGateway.findByChannelId(channelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ChannelCredentialResponse getById(Long channelId, Long id) {
        ChannelCredential credential = findAndValidateOwnership(channelId, id);
        return toResponse(credential);
    }

    @Override
    public ChannelCredentialDetailResponse getDetailById(Long channelId, Long id) {
        ChannelCredential credential = findAndValidateOwnership(channelId, id);
        return toDetailResponse(credential);
    }

    @Override
    @Transactional
    public ChannelCredentialResponse update(ChannelCredentialUpdateRequest request) {
        ChannelCredential credential = findAndValidateOwnership(request.channelId(), request.id());

        if (request.weight() != null) {
            credential.setWeight(request.weight());
        }
        if (request.priority() != null) {
            credential.setPriority(request.priority());
        }
        if (request.state() != null) {
            credential.setState(request.state());
        }
        // 替换 API Key
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            String newKey = request.apiKey().trim();
            String keyPrefix = newKey.substring(0, Math.min(8, newKey.length()));
            credential.setApiKeyPlain(newKey);
            credential.setApiKeyPrefix(keyPrefix);
        }

        ChannelCredential saved = channelCredentialGateway.save(credential);
        log.info("Updated ChannelCredential: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long channelId, Long id) {
        findAndValidateOwnership(channelId, id);
        channelCredentialGateway.deleteById(id);
        log.info("Deleted ChannelCredential: id={}", id);
    }

    @Override
    public ApiKeyTestResponse testApiKey(Long channelId, Long id) {
        // 验证归属关系
        ChannelCredential credential = findAndValidateOwnership(channelId, id);

        // TODO: 实现真实的 API Key 测试逻辑
        // 1. 获取 API Key 明文
        // 2. 获取渠道端点配置
        // 3. 发送测试请求
        // 4. 返回测试结果

        log.info("Testing ChannelCredential: id={}, channelId={}", id, channelId);

        return ApiKeyTestResponse.builder()
                .success(true)
                .latency(100L)
                .modelName("gpt-4o")
                .responsePreview("Hello! How can I assist you today?")
                .testedAt(Instant.now())
                .build();
    }

    /**
     * 验证归属关系并返回实体
     */
    private ChannelCredential findAndValidateOwnership(Long channelId, Long id) {
        ChannelCredential credential = channelCredentialGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelCredential", id));
        if (!credential.getChannelId().equals(channelId)) {
            throw new ResourceNotFoundException("ChannelCredential", id);
        }
        return credential;
    }

    private ChannelCredentialResponse toResponse(ChannelCredential credential) {
        return new ChannelCredentialResponse(
                credential.getId(),
                credential.getChannelId(),
                credential.getApiKeyPrefix(),
                credential.getApiKeyPlain(),
                credential.getName(),
                null, // description not in ChannelCredential
                credential.getWeight(),
                credential.getPriority(),
                credential.getState(),
                credential.getCreatedAt(),
                credential.getUpdatedAt()
        );
    }

    private ChannelCredentialDetailResponse toDetailResponse(ChannelCredential credential) {
        return new ChannelCredentialDetailResponse(
                credential.getId(),
                credential.getChannelId(),
                credential.getApiKeyPrefix(),
                credential.getApiKeyPlain(),
                credential.getName(),
                null, // description not in ChannelCredential
                credential.getWeight(),
                credential.getPriority(),
                credential.getState(),
                credential.getCreatedAt(),
                credential.getUpdatedAt()
        );
    }
}
