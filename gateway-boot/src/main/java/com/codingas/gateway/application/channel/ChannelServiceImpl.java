package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelEndpointRequest;
import com.codingas.gateway.application.channel.dto.ChannelEndpointResponse;
import com.codingas.gateway.application.channel.dto.ChannelRequest;
import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
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
    private final ChannelEndpointGateway channelEndpointGateway;
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
    public List<ChannelResponse> getAll() {
        return channelGateway.findAll().stream()
            .map(this::toResponse)
            .toList();
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
        // 查询端点列表
        response.setEndpoints(
            channelEndpointGateway.findByChannelId(channel.getId()).stream()
                .map(this::toEndpointResponse)
                .toList()
        );
        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        return response;
    }

    private ChannelEndpointResponse toEndpointResponse(ChannelEndpoint endpoint) {
        ChannelEndpointResponse resp = new ChannelEndpointResponse();
        resp.setId(endpoint.getId());
        resp.setChannelId(endpoint.getChannelId());
        resp.setProtocol(endpoint.getProtocol().getCode());
        resp.setEndpointUrl(endpoint.getEndpointUrl());
        resp.setState(endpoint.getState().getCode());
        resp.setCreatedAt(endpoint.getCreatedAt());
        resp.setUpdatedAt(endpoint.getUpdatedAt());
        return resp;
    }

    /**
     * 添加渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpointResponse addEndpoint(Long channelId, ChannelEndpointRequest request) {
        channelGateway.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + channelId));

        Protocol protocol = Protocol.fromCode(request.getProtocol());
        if (channelEndpointGateway.findByChannelIdAndProtocol(channelId, protocol).isPresent()) {
            throw new IllegalArgumentException("渠道下已存在该协议端点: " + request.getProtocol());
        }

        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(protocol);
        endpoint.setEndpointUrl(request.getEndpointUrl());
        endpoint.setState(ChannelEndpointState.ACTIVE);

        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        log.info("Added endpoint to channel: channelId={}, endpointId={}, protocol={}",
                channelId, saved.getId(), protocol);
        return toEndpointResponse(saved);
    }

    /**
     * 删除渠道端点
     */
    @Override
    @Transactional
    public void removeEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        channelEndpointGateway.deleteById(endpointId);
        log.info("Removed endpoint: channelId={}, endpointId={}", channelId, endpointId);
    }

    /**
     * 启用渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpointResponse enableEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        endpoint.enable();
        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        return toEndpointResponse(saved);
    }

    /**
     * 禁用渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpointResponse disableEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        endpoint.disable();
        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        return toEndpointResponse(saved);
    }
}
