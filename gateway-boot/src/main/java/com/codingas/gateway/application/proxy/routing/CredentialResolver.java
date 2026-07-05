package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 凭证解析器 — 根据 channelId 查找可用凭证
 */
@Component
@RequiredArgsConstructor
public class CredentialResolver {

    private final ChannelCredentialGateway channelCredentialGateway;

    /**
     * 根据 channelId 解析可用的 API Key
     *
     * <p>优先使用默认凭证，其次选择第一个可用凭证。</p>
     *
     * @param channelId 渠道 ID
     * @return 已解密的 API Key
     * @throws ResourceNotFoundException 未找到可用凭证
     */
    public String resolve(Long channelId) {
        // 优先使用默认凭证
        var defaultKey = channelCredentialGateway.findDefaultByChannelId(channelId);
        if (defaultKey.isPresent() && defaultKey.get().isAvailable()) {
            return defaultKey.get().getApiKeyPlain();
        }

        // 其次选择活跃凭证
        List<ChannelCredential> activeKeys = channelCredentialGateway.findActiveByChannelId(channelId);
        return activeKeys.stream()
                .findFirst()
                .map(ChannelCredential::getApiKeyPlain)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelCredential", channelId));
    }

    /**
     * 根据 channelId 解析所有可用凭证（按优先级排序）
     *
     * <p>用于渠道级故障转移，按 priority 升序排列。</p>
     *
     * @param channelId 渠道 ID
     * @return 可用凭证列表（已按优先级排序）
     */
    public List<ChannelCredential> resolveAll(Long channelId) {
        List<ChannelCredential> activeKeys = channelCredentialGateway.findActiveByChannelId(channelId);
        activeKeys.sort(Comparator.comparingInt(ChannelCredential::getPriority));
        return activeKeys;
    }
}
