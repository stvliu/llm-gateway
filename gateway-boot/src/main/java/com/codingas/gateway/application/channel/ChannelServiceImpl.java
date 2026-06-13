package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelStateTransitionRequest;
import com.codingas.gateway.application.channel.dto.ChannelEndpointRequest;
import com.codingas.gateway.application.channel.dto.ChannelEndpointResponse;
import com.codingas.gateway.application.channel.dto.ChannelRequest;
import com.codingas.gateway.application.channel.dto.ChannelResponse;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
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
    private final ChannelCredentialGateway channelCredentialGateway;
    private final ModelInstanceGateway modelInstanceGateway;
    private final ProviderGateway providerGateway;

    @Override
    @Transactional
    public ChannelResponse create(ChannelRequest request) {
        if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
            throw new GatewayRequestException("CHANNEL_NAME_DUPLICATE", "渠道名称已存在: " + request.getName());
        }

        Channel channel = new Channel();
        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getBillingMode()));
        channel.setQuotaLimit(request.getQuotaLimit());
        channel.setTimeout(request.getTimeout());
        channel.setMaxRetries(request.getMaxRetries());
        channel.setState(Channel.State.ACTIVE);

        Channel saved = channelGateway.save(channel);
        log.info("Created channel: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ChannelResponse update(Long id, ChannelRequest request) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));

        if (!channel.getName().equals(request.getName())) {
            if (channelGateway.existsByProviderIdAndName(request.getProviderId(), request.getName())) {
                throw new GatewayRequestException("CHANNEL_NAME_DUPLICATE", "渠道名称已存在: " + request.getName());
            }
        }

        channel.setProviderId(request.getProviderId());
        channel.setName(request.getName());
        channel.setBillingMode(BillingMode.fromCode(request.getBillingMode()));
        channel.setQuotaLimit(request.getQuotaLimit());
        channel.setTimeout(request.getTimeout());
        channel.setMaxRetries(request.getMaxRetries());

        Channel saved = channelGateway.save(channel);
        log.info("Updated channel: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public ChannelResponse getById(Long id) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
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

    @Override
    @Transactional
    public void setState(Long id, ChannelStateTransitionRequest request) {
        Channel channel = channelGateway.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));

        Channel.State currentState = channel.getState();
        Channel.State targetState = Channel.State.valueOf(request.getTargetState());

        // 校验状态转换合法性
        if (!currentState.canTransitionTo(targetState)) {
            throw new GatewayRequestException("INVALID_STATE_TRANSITION",
                String.format("不允许从 %s 转换为 %s", currentState, targetState));
        }

        // PENDING→ACTIVE：强制前置校验
        if (currentState == Channel.State.PENDING && targetState == Channel.State.ACTIVE) {
            validateActivationPrerequisites(channel.getId());
        }

        // PENDING→ACTIVE：级联激活 PENDING 状态的 ModelInstance
        if (currentState == Channel.State.PENDING && targetState == Channel.State.ACTIVE) {
            cascadeActivateModelInstances(channel.getId());
        }

        // SUSPENDED→ACTIVE：检查完整性（仅警告不阻塞）
        if (currentState == Channel.State.SUSPENDED && targetState == Channel.State.ACTIVE) {
            checkSuspendedActivationReadiness(channel.getId());
        }

        channel.setState(targetState);
        channelGateway.save(channel);
        log.info("渠道状态转换: id={}, {}→{}, reason={}", id, currentState, targetState, request.getReason());
    }

    /**
     * 校验 PENDING→ACTIVE 前置条件
     * <p>要求至少 1 个 Endpoint + 1 个 Credential + 1 个 ModelInstance。</p>
     */
    private void validateActivationPrerequisites(Long channelId) {
        List<ChannelEndpoint> endpoints = channelEndpointGateway.findByChannelId(channelId);
        if (endpoints.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_ENDPOINT", "请先添加端点");
        }

        List<ChannelCredential> credentials = channelCredentialGateway.findByChannelId(channelId);
        if (credentials.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_CREDENTIAL", "请先添加凭证");
        }

        List<ModelInstance> instances = modelInstanceGateway.findByChannelId(channelId);
        if (instances.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_MODEL_INSTANCE", "请先关联模型实例");
        }
    }

    /**
     * 级联激活 PENDING 状态的 ModelInstance
     * <p>将同 Channel 下所有 PENDING 状态的 ModelInstance 设为 ACTIVE。</p>
     */
    private void cascadeActivateModelInstances(Long channelId) {
        List<ModelInstance> pendingInstances = modelInstanceGateway.findByChannelId(channelId).stream()
            .filter(mi -> mi.getState() == ModelInstance.State.PENDING)
            .toList();
        if (!pendingInstances.isEmpty()) {
            pendingInstances.forEach(mi -> mi.setState(ModelInstance.State.ACTIVE));
            modelInstanceGateway.saveAll(pendingInstances);
            log.info("级联激活 {} 个 PENDING 模型实例: channelId={}", pendingInstances.size(), channelId);
        }
    }

    /**
     * 检查 SUSPENDED→ACTIVE 完整性
     * <p>仅警告不阻塞，提示管理员检查 Endpoint/Credential/ModelInstance 是否仍完整。</p>
     */
    private void checkSuspendedActivationReadiness(Long channelId) {
        List<ChannelEndpoint> endpoints = channelEndpointGateway.findByChannelId(channelId);
        List<ChannelCredential> credentials = channelCredentialGateway.findByChannelId(channelId);
        List<ModelInstance> instances = modelInstanceGateway.findByChannelId(channelId);

        if (endpoints.isEmpty()) {
            log.warn("渠道恢复激活但无端点: channelId={}", channelId);
        }
        if (credentials.isEmpty()) {
            log.warn("渠道恢复激活但无凭证: channelId={}", channelId);
        }
        if (instances.isEmpty()) {
            log.warn("渠道恢复激活但无模型实例: channelId={}", channelId);
        }
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
        response.setTimeout(channel.getTimeout());
        response.setMaxRetries(channel.getMaxRetries());
        response.setState(channel.getState().name());
        // 查询端点列表
        response.setEndpoints(
            channelEndpointGateway.findByChannelId(channel.getId()).stream()
                .map(this::toEndpointResponse)
                .toList()
        );
        response.setCreatedAt(channel.getCreatedAt());
        response.setUpdatedAt(channel.getUpdatedAt());
        // 健康状态字段透传（last-write-wins，未测试过时为 null）
        response.setLastHealthCheckAt(channel.getLastHealthCheckAt());
        response.setLastHealthStatus(channel.getLastHealthStatus());
        response.setLastHealthSource(channel.getLastHealthSource());
        return response;
    }

    private void validateEndpointRequest(Long channelId, Long excludeEndpointId, ChannelEndpointRequest request) {
        if (request.getProtocol() == null || request.getProtocol().isBlank()) {
            throw new IllegalArgumentException("协议不能为空");
        }
        Protocol protocol = Protocol.fromCode(request.getProtocol());
        if (request.getEndpointUrl() == null || request.getEndpointUrl().isBlank()) {
            throw new IllegalArgumentException("端点 URL 不能为空");
        }
        String normalizedUrl = request.getEndpointUrl().trim();
        List<ChannelEndpoint> existing = channelEndpointGateway.findByChannelId(channelId);
        // 同渠道下同协议唯一（排除自身）
        if (existing.stream()
                .filter(ep -> !ep.getId().equals(excludeEndpointId))
                .anyMatch(ep -> ep.getProtocol().equals(protocol))) {
            throw new IllegalArgumentException("渠道下已存在该协议端点: " + request.getProtocol());
        }
        // 同渠道下 URL 唯一（排除自身）
        if (existing.stream()
                .filter(ep -> !ep.getId().equals(excludeEndpointId))
                .anyMatch(ep -> ep.getEndpointUrl().equals(normalizedUrl))) {
            throw new IllegalArgumentException("渠道下已存在相同 URL 的端点: " + normalizedUrl);
        }
    }

    private ChannelEndpointResponse toEndpointResponse(ChannelEndpoint endpoint) {
        ChannelEndpointResponse resp = new ChannelEndpointResponse();
        resp.setId(endpoint.getId());
        resp.setChannelId(endpoint.getChannelId());
        resp.setProtocol(endpoint.getProtocol().getCode());
        resp.setEndpointUrl(endpoint.getEndpointUrl());
        resp.setCreatedAt(endpoint.getCreatedAt());
        resp.setUpdatedAt(endpoint.getUpdatedAt());
        return resp;
    }

    /**
     * 添加渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpointResponse addEndpoint(ChannelEndpointRequest request) {
        Long channelId = request.getChannelId();
        channelGateway.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + channelId));

        validateEndpointRequest(channelId, null, request);

        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(Protocol.fromCode(request.getProtocol()));
        endpoint.setEndpointUrl(request.getEndpointUrl().trim());

        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        log.info("Added endpoint to channel: channelId={}, endpointId={}, protocol={}",
                channelId, saved.getId(), saved.getProtocol());
        return toEndpointResponse(saved);
    }

    /**
     * 更新渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpointResponse updateEndpoint(Long channelId, Long endpointId, ChannelEndpointRequest request) {
        ChannelEndpoint endpoint = channelEndpointGateway.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }

        validateEndpointRequest(channelId, endpointId, request);

        endpoint.setProtocol(Protocol.fromCode(request.getProtocol()));
        endpoint.setEndpointUrl(request.getEndpointUrl().trim());
        ChannelEndpoint saved = channelEndpointGateway.save(endpoint);
        log.info("Updated endpoint: channelId={}, endpointId={}", channelId, endpointId);
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
}