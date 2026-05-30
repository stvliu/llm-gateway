package com.codingas.gateway.application.model;

import com.codingas.gateway.application.model.dto.ModelDiscoveryResponse;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.ModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型发现服务
 *
 * <p>根据用户所属团队的渠道，返回可用的模型列表。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDiscoveryService {

    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;
    private final ChannelModelGateway channelModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 获取用户可见的模型列表
     *
     * @param userId 用户 ID（已认证的身份）
     * @return 兼容 OpenAI 格式的模型列表
     */
    public ModelDiscoveryResponse getVisibleModels(Long userId) {
        // 通过用户查找所属团队
        Long teamId = userTeamGateway.findTeamIdByUserId(userId);
        if (teamId == null) {
            log.debug("用户未关联任何团队: userId={}", userId);
            return new ModelDiscoveryResponse("list", List.of());
        }

        // 通过团队查找可访问的渠道
        List<Long> channelIds = teamChannelGateway.findChannelIdsByTeamId(teamId);
        if (channelIds.isEmpty()) {
            log.debug("团队未关联任何渠道: teamId={}", teamId);
            return new ModelDiscoveryResponse("list", List.of());
        }

        // 通过渠道查找可用模型
        List<Model> visibleModels = channelIds.stream()
                .flatMap(channelId -> channelModelGateway.findActiveByChannelId(channelId).stream())
                .map(cm -> modelGateway.findById(cm.getModelId()).orElse(null))
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

        log.debug("模型发现: userId={}, teamId={}, visibleModels={}", userId, teamId, items.size());
        return new ModelDiscoveryResponse("list", items);
    }
}