package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
     * @param channelId 通道 ID
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
}
