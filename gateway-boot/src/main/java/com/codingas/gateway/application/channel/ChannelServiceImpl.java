package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelRequest;
import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道应用服务实现
 *
 * <p>管理渠道（Channel）的 CRUD 操作。</p>
 * <p>定价已下沉到 ChannelModel，Channel 只持有连接和路由相关字段。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelServiceImpl implements ChannelService {

    private final ChannelGateway channelGateway;
    private final ProviderGateway providerGateway;

    @Override
    @Transactional
    public ChannelResponse create(ChannelRequest request) {
        if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
            throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
        }

        Channel channel = new Channel();
        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getBillingMode()));
        channel.setQuotaLimit(request.getQuotaLimit());
        channel.setPriority(request.getPriority());
        channel.setWeight(request.getWeight());
        channel.setTimeout(request.getTimeout());
        channel.setMaxRetries(request.getMaxRetries());
        channel.setState(ChannelState.ACTIVE);

        Channel saved = channelGateway.save(channel);
        log.info("Created channel: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ChannelResponse update(Long id, ChannelRequest request) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + id));

        if (!channel.getName().equals(request.getName())) {
            if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
                throw new IllegalArgumentException("渠道名称已存在: " + request.getName());
            }
        }

        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getBillingMode()));
        channel.setQuotaLimit(request.getQuotaLimit());
        channel.setPriority(request.getPriority());
        channel.setWeight(request.getWeight());
        channel.setTimeout(request.getTimeout());
        channel.setMaxRetries(request.getMaxRetries());

        Channel saved = channelGateway.save(channel);
        log.info("Updated channel: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public ChannelResponse getById(Long id) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + id));
        return toResponse(channel);
    }

    @Override
    public List<ChannelResponse> getByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<ChannelResponse> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode) {
        return channelGateway.findByProviderIdAndBillingMode(providerId, billingMode).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        channelGateway.deleteById(id);
        log.info("Deleted channel: id={}", id);
    }

    private ChannelResponse toResponse(Channel channel) {
        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getId());
        response.setProviderId(channel.getProviderId());
        // 从 Provider 查找名称（仅展示用）
        providerGateway.findById(channel.getProviderId())
            .ifPresent(p -> response.setProviderName(p.getName()));
        response.setName(channel.getName());
        response.setBillingMode(channel.getBillingMode().getCode());
        response.setQuotaLimit(channel.getQuotaLimit());
        response.setPriority(channel.getPriority());
        response.setWeight(channel.getWeight());
        response.setTimeout(channel.getTimeout());
        response.setMaxRetries(channel.getMaxRetries());
        response.setState(channel.getState().getCode());
        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        return response;
    }
}
