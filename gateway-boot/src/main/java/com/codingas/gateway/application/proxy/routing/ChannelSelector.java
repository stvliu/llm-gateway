package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通道选择器 — 根据 modelId 查找可用通道，按路由策略选择
 */
@Component
@RequiredArgsConstructor
public class ChannelSelector {

    private final ChannelModelGateway channelModelGateway;
    private final ChannelGateway channelGateway;

    /**
     * 根据 modelId 选择可用通道
     *
     * <p>查找所有活跃的 ChannelModel，过滤出对应的活跃 Channel，按优先级选择第一个。</p>
     *
     * @param modelId 模型 ID
     * @return 选中的 ChannelModel（包含 channelId 和 modelId）
     * @throws ResourceNotFoundException 无可用通道
     */
    public ChannelModel select(Long modelId) {
        List<ChannelModel> channelModels = channelModelGateway.findActiveByModelId(modelId);

        if (channelModels.isEmpty()) {
            throw new ResourceNotFoundException("ChannelModel", modelId);
        }

        // 批量查询所有关联的 Channel，避免 N+1 问题
        List<Long> channelIds = channelModels.stream().map(ChannelModel::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() == ChannelState.ACTIVE)
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        List<ChannelModel> activeModels = channelModels.stream()
                .filter(cm -> activeChannelIds.contains(cm.getChannelId()))
                .toList();

        if (activeModels.isEmpty()) {
            throw new ResourceNotFoundException("ChannelModel", modelId);
        }

        // 按优先级返回第一个（后续可扩展为 WEIGHTED/ROUND_ROBIN/FAILOVER）
        return activeModels.getFirst();
    }
}
