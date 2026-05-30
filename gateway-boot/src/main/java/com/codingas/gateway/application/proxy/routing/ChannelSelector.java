package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通道选择器 — 根据 modelId 和用户团队权限查找可用通道
 */
@Component
@RequiredArgsConstructor
public class ChannelSelector {

    private final ChannelModelGateway channelModelGateway;
    private final ChannelGateway channelGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;

    /**
     * 根据 modelId 和用户团队权限选择可用通道
     *
     * <p>查找所有活跃的 ChannelModel，过滤出对应的活跃 Channel，按优先级选择第一个。</p>
     * <p>同时根据用户所属团队的渠道权限进行过滤。</p>
     *
     * @param modelId 模型 ID
     * @param userId  用户 ID（用于团队渠道权限过滤）
     * @return 选中的 ChannelModel（包含 channelId 和 modelId）
     * @throws ResourceNotFoundException 无可用通道
     */
    public ChannelModel select(Long modelId, Long userId) {
        // 获取用户团队渠道集合
        Long teamId = userTeamGateway.findTeamIdByUserId(userId);
        List<Long> teamChannelIds = teamId != null
                ? teamChannelGateway.findChannelIdsByTeamId(teamId)
                : List.of();

        List<ChannelModel> channelModels = channelModelGateway.findActiveByModelId(modelId);
        if (channelModels.isEmpty()) {
            throw new ResourceNotFoundException("ChannelModel", modelId);
        }

        // 过滤：只保留团队渠道内的 ChannelModel
        List<ChannelModel> permittedModels = teamChannelIds.isEmpty()
                ? List.of()
                : channelModels.stream()
                        .filter(cm -> teamChannelIds.contains(cm.getChannelId()))
                        .toList();

        // 再过滤活跃 Channel
        List<Long> channelIds = permittedModels.stream().map(ChannelModel::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() == ChannelState.ACTIVE)
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        List<ChannelModel> activeModels = permittedModels.stream()
                .filter(cm -> activeChannelIds.contains(cm.getChannelId()))
                .toList();

        if (activeModels.isEmpty()) {
            throw new ResourceNotFoundException("ChannelModel", modelId);
        }

        return activeModels.getFirst();
    }
}