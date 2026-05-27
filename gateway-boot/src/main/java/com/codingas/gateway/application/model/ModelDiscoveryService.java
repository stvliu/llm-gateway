package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型发现服务
 *
 * <p>根据 API Key 可见的渠道，返回可用的模型列表。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final UserApiKeyGateway userApiKeyGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 获取 API Key 可见的模型列表
     *
     * @param apiKeyId API Key ID（已认证的身份）
     * @return 兼容 OpenAI 格式的模型列表
     */
    public ModelDiscoveryResponse getVisibleModels(Long apiKeyId) {
        var apiKey = userApiKeyGateway.findById(apiKeyId)
                .orElseThrow(() -> new GatewayRequestException("API_KEY_NOT_FOUND", "API Key 不存在"));

        List<Long> channelIds = apiKey.getChannelIds();
        if (channelIds == null || channelIds.isEmpty()) {
            return new ModelDiscoveryResponse("list", List.of());
        }

        List<Model> visibleModels = channelIds.stream()
                .flatMap(channelId -> channelModelGateway.findActiveByChannelId(channelId).stream())
                .map(cm -> {
                    var modelOpt = modelGateway.findById(cm.getModelId());
                    if (modelOpt.isEmpty()) {
                        log.warn("模型引用不存在: modelId={}, channelId={}", cm.getModelId(), cm.getChannelId());
                    }
                    return modelOpt.orElse(null);
                })
                .filter(m -> m != null && ModelState.ACTIVE.equals(m.getState()))
                .distinct()
                .toList();

        List<ModelDiscoveryResponse.ModelItem> items = visibleModels.stream()
                .map(m -> new ModelDiscoveryResponse.ModelItem(
                        m.getModelName(),
                        "model",
                        m.getCreatedAt() != null ? m.getCreatedAt().getEpochSecond() : 0L,
                        "system"
                ))
                .toList();

        log.debug("模型发现: apiKeyId={}, visibleModels={}", apiKeyId, items.size());
        return new ModelDiscoveryResponse("list", items);
    }
}