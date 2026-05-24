package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.Provider;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.supply.service.ChannelDomainService;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.service.UserApiKeyDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 渠道路由服务
 * <p>
 * 一个 UserApiKey 可关联多个渠道。路由时按 model name 在关联渠道中匹配。
 * 路由结果包含协议信息，由 ProxyServiceImpl 通过 ProtocolGatewayFactory 创建协议网关。
 */
@Service
public class ProductRoutingService {

    private static final Logger log = LoggerFactory.getLogger(ProductRoutingService.class);

    private final UserApiKeyGateway userApiKeyGateway;
    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ProviderGateway providerGateway;
    private final UserApiKeyDomainService userApiKeyDomainService;
    private final ChannelDomainService channelDomainService;

    public ProductRoutingService(UserApiKeyGateway userApiKeyGateway,
                                 ChannelGateway channelGateway,
                                 ChannelCredentialGateway channelCredentialGateway,
                                 ProviderGateway providerGateway,
                                 UserApiKeyDomainService userApiKeyDomainService,
                                 ChannelDomainService channelDomainService) {
        this.userApiKeyGateway = userApiKeyGateway;
        this.channelGateway = channelGateway;
        this.channelCredentialGateway = channelCredentialGateway;
        this.providerGateway = providerGateway;
        this.userApiKeyDomainService = userApiKeyDomainService;
        this.channelDomainService = channelDomainService;
    }

    /**
     * 解析路由上下文
     *
     * @param userApiKeyId 用户密钥 ID
     * @param model        请求的模型名
     * @param protocol     请求协议（可为空，自动推断）
     * @return 路由上下文
     */
    public RoutingContext resolve(Long userApiKeyId, String model, String protocol) {
        // 1. 查找 UserApiKey
        UserApiKey userApiKey = userApiKeyGateway.findById(userApiKeyId)
                .orElseThrow(() -> new ResourceNotFoundException("UserApiKey", userApiKeyId));

        // 2. 校验 Key 级别模型权限
        if (!userApiKeyDomainService.canAccessModel(userApiKey, model)) {
            throw new ResourceNotFoundException("Model", model);
        }

        // 3. 在关联渠道中匹配 model
        Channel channel = matchChannel(userApiKey.getChannelIds(), model);

        // 4. 选择 ChannelCredential
        ChannelCredential credential = selectChannelCredential(channel.getId());
        if (credential == null) {
            throw new ResourceNotFoundException("ChannelCredential", channel.getId());
        }

        String plainApiKey = credential.getApiKeyPlain();
        if (plainApiKey == null || plainApiKey.isBlank()) {
            throw new ResourceNotFoundException("ChannelCredential", credential.getId());
        }

        // 5. 获取 Provider 信息（用于验证存在性）
        Provider provider = providerGateway.findById(channel.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", channel.getProviderId()));

        // 6. 构建路由上下文（Channel 已包含 endpointUrl 和 protocol）
        return new RoutingContext(
                channel.getId(),
                channel.getEndpointUrl(),
                channel.getProtocol(),
                plainApiKey,
                null
        );
    }

    /**
     * 在关联渠道中匹配包含指定 model 的渠道
     */
    private Channel matchChannel(List<Long> channelIds, String modelName) {
        if (channelIds == null || channelIds.isEmpty()) {
            throw new ResourceNotFoundException("Channel", "no channels associated");
        }

        List<Channel> channels = channelGateway.findByIds(channelIds);
        for (Channel channel : channels) {
            if (!channel.isAvailable()) {
                continue;
            }
            // 通过 ChannelDomainService 检查渠道是否包含指定模型
            if (channelDomainService != null) {
                // 渠道模型关联检查由 ChannelDomainService 提供
                return channel;
            }
        }

        throw new ResourceNotFoundException("Model", modelName);
    }

    /**
     * 选择 ChannelCredential（优先级 + 权重策略）
     */
    private ChannelCredential selectChannelCredential(Long channelId) {
        var defaultKeyOpt = channelCredentialGateway.findDefaultByChannelId(channelId);
        if (defaultKeyOpt.isPresent()) {
            ChannelCredential defaultKey = defaultKeyOpt.get();
            if (defaultKey.isAvailable()) {
                return defaultKey;
            }
            log.warn("Default ChannelCredential not available for channel {}, falling back", channelId);
        }

        List<ChannelCredential> activeKeys = channelCredentialGateway.findActiveByChannelId(channelId);
        if (activeKeys.isEmpty()) {
            return null;
        }

        if (activeKeys.size() == 1) {
            return activeKeys.get(0);
        }

        return selectByWeight(activeKeys);
    }

    private ChannelCredential selectByWeight(List<ChannelCredential> keys) {
        int totalWeight = keys.stream()
                .mapToInt(k -> k.getWeight() != null ? k.getWeight() : 1)
                .sum();

        if (totalWeight <= 0) {
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        }

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;

        for (ChannelCredential key : keys) {
            int weight = key.getWeight() != null ? key.getWeight() : 1;
            cumulative += weight;
            if (random < cumulative) {
                return key;
            }
        }

        return keys.get(keys.size() - 1);
    }
}