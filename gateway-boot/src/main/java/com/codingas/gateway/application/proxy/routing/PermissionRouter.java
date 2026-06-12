package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限路由器 — 按用户团队权限过滤模型实例
 *
 * <p>ADMIN 角色跳过团队渠道过滤，可以访问所有活跃渠道。</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionRouter implements Router {

    private final ChannelGateway channelGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        // 获取用户有权限的渠道 ID 集合
        Set<Long> permittedChannelIds = getPermittedChannelIds(request);

        if (permittedChannelIds.isEmpty()) {
            return List.of();
        }

        // 过滤：只保留有权限的渠道内的实例
        List<ModelInstance> permitted = instances.stream()
                .filter(mi -> permittedChannelIds.contains(mi.getChannelId()))
                .toList();

        if (permitted.isEmpty()) {
            return List.of();
        }

        // 再过滤活跃 Channel
        List<Long> channelIds = permitted.stream().map(ModelInstance::getChannelId).toList();
        List<Channel> activeChannels = channelGateway.findByIds(channelIds).stream()
                .filter(ch -> ch.getPhase() != null && ch.getPhase().isRoutable())
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        return permitted.stream()
                .filter(mi -> activeChannelIds.contains(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }

    private Set<Long> getPermittedChannelIds(RoutingRequest request) {
        if ("ADMIN".equals(request.getRole())) {
            return channelGateway.findAll().stream()
                    .filter(ch -> ch.getPhase() != null && ch.getPhase().isRoutable())
                    .map(Channel::getId)
                    .collect(Collectors.toSet());
        }

        Long teamId = userTeamGateway.findTeamIdByUserId(request.getUserId());
        if (teamId == null) {
            return Set.of();
        }
        return new HashSet<>(teamChannelGateway.findChannelIdsByTeamId(teamId));
    }
}
