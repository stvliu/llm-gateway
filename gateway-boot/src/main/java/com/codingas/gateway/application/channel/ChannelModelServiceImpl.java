package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道模型关联应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelModelServiceImpl implements ChannelModelService {

    private final ChannelModelGateway channelModelGateway;
    private final ModelGateway modelGateway;

    /**
     * 查询指定渠道下的所有模型关联
     */
    @Transactional(readOnly = true)
    @Override
    public List<ChannelModelResponse> getModelsByChannelId(Long channelId) {
        List<ChannelModel> channelModels = channelModelGateway.findByChannelId(channelId);
        return channelModels.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 创建渠道模型关联
     */
    @Transactional
    @Override
    public ChannelModelResponse create(Long channelId, ChannelModelCreateRequest request) {
        // 检查是否已关联
        boolean exists = channelModelGateway.existsByChannelIdAndModelId(channelId, request.getModelId());
        if (exists) {
            log.warn("模型已关联到该渠道, channelId={}, modelId={}", channelId, request.getModelId());
            throw new DuplicateResourceException("ChannelModel", "modelId");
        }

        ChannelModel cm = new ChannelModel();
        cm.setChannelId(channelId);
        cm.setModelId(request.getModelId());
        cm.setUpstreamModelName(request.getUpstreamModelName());
        cm.setState(ChannelModelState.ACTIVE);
        cm = channelModelGateway.save(cm);
        log.info("渠道模型关联创建成功, id={}, channelId={}, modelId={}", cm.getId(), channelId, request.getModelId());
        return toResponse(cm);
    }

    /**
     * 删除渠道模型关联
     */
    @Transactional
    @Override
    public void delete(Long channelId, Long id) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel", id));
        if (!cm.getChannelId().equals(channelId)) {
            log.warn("模型关联不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, cm.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型关联不属于该渠道");
        }
        channelModelGateway.deleteById(id);
        log.info("渠道模型关联删除成功, id={}, channelId={}", id, channelId);
    }

    /**
     * 启用/禁用渠道模型关联
     */
    @Transactional
    @Override
    public void setEnabled(Long channelId, Long id, boolean enabled) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel", id));
        if (!cm.getChannelId().equals(channelId)) {
            log.warn("模型关联不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, cm.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型关联不属于该渠道");
        }
        cm.setState(enabled ? ChannelModelState.ACTIVE : ChannelModelState.INACTIVE);
        channelModelGateway.save(cm);
        log.info("渠道模型关联状态更新成功, id={}, channelId={}, enabled={}", id, channelId, enabled);
    }

    /**
     * 将实体转换为响应 DTO
     */
    private ChannelModelResponse toResponse(ChannelModel cm) {
        ChannelModelResponse resp = new ChannelModelResponse();
        resp.setId(cm.getId());
        resp.setChannelId(cm.getChannelId());
        resp.setModelId(cm.getModelId());
        resp.setUpstreamModelName(cm.getUpstreamModelName());
        resp.setState(cm.getState().name());

        modelGateway.findById(cm.getModelId()).ifPresent(spec -> {
            resp.setModelName(spec.getModelName());
            resp.setDisplayName(spec.getDisplayName());
            resp.setModelFamily(spec.getModelFamily());
        });

        return resp;
    }

    /**
     * 更新渠道模型关联的上游模型名
     */
    @Transactional
    @Override
    public void updateUpstreamModelName(Long channelId, Long id, String upstreamModelName) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel", id));
        if (!cm.getChannelId().equals(channelId)) {
            log.warn("模型关联不属于该渠道, id={}, channelId={}, actualChannelId={}", id, channelId, cm.getChannelId());
            throw new GatewayRequestException("CHANNEL_MISMATCH", "模型关联不属于该渠道");
        }
        cm.setUpstreamModelName(upstreamModelName);
        channelModelGateway.save(cm);
        log.info("渠道模型关联上游模型名更新成功, id={}, channelId={}, upstreamModelName={}", id, channelId, upstreamModelName);
    }
}