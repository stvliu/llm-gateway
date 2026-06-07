package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型实例选择器 — 根据 modelId 和用户团队权限查找可用模型实例
 *
 * <p>基于 ModelInstance.priority 排序选择优先级最高的实例。</p>
 * <p>从 ChannelSelector 重构而来，适配 ModelInstance 新架构。</p>
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private final ModelInstanceGateway modelInstanceGateway;
    private final ChannelGateway channelGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;

    /**
     * 根据 modelId 和用户团队权限选择可用模型实例
     *
     * <p>查找所有活跃的 ModelInstance（按 priority 升序排序），过滤出对应的活跃 Channel，
     * 选择 priority 最小的实例。</p>
     * <p>同时根据用户所属团队的渠道权限进行过滤。</p>
     *
     * @param modelId 模型 ID
     * @param userId  用户 ID（用于团队渠道权限过滤）
     * @return 选中的 ModelInstance（包含 channelId 和 modelId，priority 最优）
     * @throws ResourceNotFoundException 无可用实例
     */
    public ModelInstance select(Long modelId, Long userId) {
        // 获取用户团队渠道集合
        Long teamId = userTeamGateway.findTeamIdByUserId(userId);
        Set<Long> permittedChannelIds = teamId != null
                ? new HashSet<>(teamChannelGateway.findChannelIdsByTeamId(teamId))
                : Set.of();

        // 按 priority 升序获取活跃 ModelInstance
        List<ModelInstance> modelInstances = modelInstanceGateway.findActiveByModelIdOrderByPriority(modelId);
        if (modelInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 过滤：只保留团队渠道内的 ModelInstance
        List<ModelInstance> permittedInstances = permittedChannelIds.isEmpty()
                ? List.of()
                : modelInstances.stream()
                        .filter(mi -> permittedChannelIds.contains(mi.getChannelId()))
                        .toList();

        // 再过滤活跃 Channel
        List<Long> channelIds = permittedInstances.stream().map(ModelInstance::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() == ChannelState.ACTIVE)
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        List<ModelInstance> activeInstances = permittedInstances.stream()
                .filter(mi -> activeChannelIds.contains(mi.getChannelId()))
                .toList();

        if (activeInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 返回 priority 最小的实例（已按 priority 升序排序，取第一个）
        return activeInstances.getFirst();
    }
}