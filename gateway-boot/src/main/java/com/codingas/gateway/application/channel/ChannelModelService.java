package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道模型关联应用服务
 */
@Service
@RequiredArgsConstructor
public class ChannelModelService {

    private final ChannelModelGateway channelModelGateway;
    private final ModelSpecGateway modelSpecGateway;

    /**
     * 查询指定渠道下的所有模型关联
     */
    @Transactional(readOnly = true)
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
    public ChannelModelResponse create(Long channelId, ChannelModelCreateRequest request) {
        // 检查是否已关联
        boolean exists = channelModelGateway.existsByChannelIdAndModelId(channelId, request.getModelSpecId());
        if (exists) {
            throw new IllegalArgumentException("模型已关联到该渠道");
        }

        ChannelModel cm = new ChannelModel();
        cm.setChannelId(channelId);
        cm.setModelSpecId(request.getModelSpecId());
        cm.setState(ChannelModelState.ACTIVE);
        cm = channelModelGateway.save(cm);
        return toResponse(cm);
    }

    /**
     * 删除渠道模型关联
     */
    @Transactional
    public void delete(Long channelId, Long id) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel", id));
        if (!cm.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("模型关联不属于该渠道");
        }
        channelModelGateway.deleteById(id);
    }

    /**
     * 启用/禁用渠道模型关联
     */
    @Transactional
    public void setEnabled(Long channelId, Long id, boolean enabled) {
        ChannelModel cm = channelModelGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChannelModel", id));
        if (!cm.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("模型关联不属于该渠道");
        }
        cm.setState(enabled ? ChannelModelState.ACTIVE : ChannelModelState.INACTIVE);
        channelModelGateway.save(cm);
    }

    /**
     * 将实体转换为响应 DTO
     */
    private ChannelModelResponse toResponse(ChannelModel cm) {
        ChannelModelResponse resp = new ChannelModelResponse();
        resp.setId(cm.getId());
        resp.setChannelId(cm.getChannelId());
        resp.setModelSpecId(cm.getModelSpecId());
        resp.setState(cm.getState().name());

        modelSpecGateway.findById(cm.getModelSpecId()).ifPresent(spec -> {
            resp.setProviderModelId(spec.getProviderModelId());
            resp.setDisplayName(spec.getDisplayName());
            resp.setModelFamily(spec.getModelFamily());
        });

        return resp;
    }
}