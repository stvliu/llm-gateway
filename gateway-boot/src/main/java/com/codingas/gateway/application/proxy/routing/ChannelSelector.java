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

/**
 * 通道选择器 — 根据 modelSpecId 查找可用通道，按路由策略选择
 */
@Component
@RequiredArgsConstructor
public class ChannelSelector {

    private final ChannelModelGateway channelModelGateway;
    private final ChannelGateway channelGateway;

    /**
     * 根据 modelSpecId 选择可用通道
     *
     * <p>查找所有活跃的 ChannelModel，过滤出对应的活跃 Channel，按优先级选择第一个。</p>
     *
     * @param modelSpecId 模型规格 ID
     * @return 选中的 ChannelModel（包含 channelId 和 modelSpecId）
     * @throws ResourceNotFoundException 无可用通道
     */
    public ChannelModel select(Long modelSpecId) {
        List<ChannelModel> channelModels = channelModelGateway.findActiveByModelSpecId(modelSpecId);

        // 过滤出通道本身也是活跃的
        List<ChannelModel> activeModels = channelModels.stream()
                .filter(cm -> {
                    Channel ch = channelGateway.findById(cm.getChannelId()).orElse(null);
                    return ch != null && ch.getState() == ChannelState.ACTIVE;
                })
                .toList();

        if (activeModels.isEmpty()) {
            throw new ResourceNotFoundException("ChannelModel", modelSpecId);
        }

        // 按优先级返回第一个（后续可扩展为 WEIGHTED/ROUND_ROBIN/FAILOVER）
        return activeModels.getFirst();
    }
}
