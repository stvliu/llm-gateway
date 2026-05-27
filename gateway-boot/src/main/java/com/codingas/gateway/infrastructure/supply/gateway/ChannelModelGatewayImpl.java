package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;
import com.codingas.gateway.domain.supply.gateway.ChannelModelGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelModelDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 渠道模型持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelModelGatewayImpl implements ChannelModelGateway {

    private final ChannelModelRepository channelModelRepository;

    @Override
    public ChannelModel save(ChannelModel channelModel) {
        ChannelModelDo doObj = toDo(channelModel);
        ChannelModelDo saved = channelModelRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ChannelModel> findById(Long id) {
        return channelModelRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ChannelModel> findByChannelId(Long channelId) {
        return channelModelRepository.findByChannelId(channelId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelModel> findActiveByChannelId(Long channelId) {
        return channelModelRepository.findByChannelIdAndState(channelId, ChannelModelState.ACTIVE.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelModel> findActiveByModelId(Long modelId) {
        return channelModelRepository.findByModelIdAndState(modelId, ChannelModelState.ACTIVE.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelModel> findByChannelIdAndState(Long channelId, ChannelModelState state) {
        return channelModelRepository.findByChannelIdAndState(channelId, state.name())
                .stream().map(this::toEntity).toList();
    }

    @Override
    public List<ChannelModel> findByIds(List<Long> ids) {
        return channelModelRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        channelModelRepository.deleteById(id);
    }

    @Override
    public boolean existsByChannelIdAndModelId(Long channelId, Long modelId) {
        return channelModelRepository.findByChannelId(channelId).stream()
                .anyMatch(cm -> cm.getModelId().equals(modelId));
    }

    @Override
    public List<ChannelModel> saveAll(List<ChannelModel> channelModels) {
        List<ChannelModelDo> doList = channelModels.stream().map(this::toDo).toList();
        return channelModelRepository.saveAll(doList).stream().map(this::toEntity).toList();
    }

    private ChannelModel toEntity(ChannelModelDo doObj) {
        ChannelModel entity = new ChannelModel();
        entity.setId(doObj.getId());
        entity.setChannelId(doObj.getChannelId());
        entity.setModelId(doObj.getModelId());
        entity.setUpstreamModelName(doObj.getUpstreamModelName());
        entity.setInputPrice(doObj.getInputPrice());
        entity.setOutputPrice(doObj.getOutputPrice());
        entity.setReasoningPrice(doObj.getReasoningPrice());
        entity.setCacheReadPrice(doObj.getCacheReadPrice());
        entity.setCacheWritePrice(doObj.getCacheWritePrice());
        entity.setInputAudioPrice(doObj.getInputAudioPrice());
        entity.setOutputAudioPrice(doObj.getOutputAudioPrice());
        entity.setQuotaLimit(doObj.getQuotaLimit());
        entity.setState(ChannelModelState.valueOf(doObj.getState()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ChannelModelDo toDo(ChannelModel entity) {
        ChannelModelDo doObj = new ChannelModelDo();
        doObj.setId(entity.getId());
        doObj.setChannelId(entity.getChannelId());
        doObj.setModelId(entity.getModelId());
        doObj.setUpstreamModelName(entity.getUpstreamModelName());
        doObj.setInputPrice(entity.getInputPrice());
        doObj.setOutputPrice(entity.getOutputPrice());
        doObj.setReasoningPrice(entity.getReasoningPrice());
        doObj.setCacheReadPrice(entity.getCacheReadPrice());
        doObj.setCacheWritePrice(entity.getCacheWritePrice());
        doObj.setInputAudioPrice(entity.getInputAudioPrice());
        doObj.setOutputAudioPrice(entity.getOutputAudioPrice());
        doObj.setQuotaLimit(entity.getQuotaLimit());
        doObj.setState(entity.getState() != null ? entity.getState().name() : ChannelModelState.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
