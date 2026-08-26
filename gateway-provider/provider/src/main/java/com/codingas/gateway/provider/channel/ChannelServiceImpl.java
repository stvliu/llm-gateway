/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.BillingMode;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.vendor.Provider;
import com.codingas.gateway.provider.vendor.ProviderRepository;
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

    private final ChannelRepository channelRepository;
    private final ChannelEndpointRepository channelEndpointRepository;
    private final ChannelCredentialRepository channelCredentialRepository;
    private final ModelInstanceRepository modelInstanceRepository;
    private final ProviderRepository providerRepository;

    @Override
    @Transactional
    public Channel create(ChannelCommand command) {
        if (channelRepository.existsByProviderIdAndName(command.getProviderId(), command.getName())) {
            throw new GatewayRequestException("CHANNEL_NAME_DUPLICATE", "渠道名称已存在: " + command.getName());
        }

        Channel channel = new Channel();
        channel.setProviderId(command.getProviderId());
        channel.setName(command.getName());
        channel.setBillingMode(BillingMode.fromCode(command.getBillingMode()));
        channel.setQuotaLimit(command.getQuotaLimit());
        channel.setTimeout(command.getTimeout());
        channel.setMaxRetries(command.getMaxRetries());
        channel.setState(ChannelState.ACTIVE);

        Channel saved = channelRepository.save(channel);
        log.info("Created channel: id={}, name={}", saved.getId(), saved.getName());

        return saved;
    }

    @Override
    @Transactional
    public Channel update(Long id, ChannelCommand command) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));

        if (!channel.getName().equals(command.getName())) {
            if (channelRepository.existsByProviderIdAndName(command.getProviderId(), command.getName())) {
                throw new GatewayRequestException("CHANNEL_NAME_DUPLICATE", "渠道名称已存在: " + command.getName());
            }
        }

        channel.setProviderId(command.getProviderId());
        channel.setName(command.getName());
        channel.setBillingMode(BillingMode.fromCode(command.getBillingMode()));
        channel.setQuotaLimit(command.getQuotaLimit());
        channel.setTimeout(command.getTimeout());
        channel.setMaxRetries(command.getMaxRetries());

        Channel saved = channelRepository.save(channel);
        log.info("Updated channel: id={}", saved.getId());

        return saved;
    }

    @Override
    public Channel getById(Long id) {
        return channelRepository.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
    }

    @Override
    public List<Channel> getAll() {
        return channelRepository.findAll();
    }

    @Override
    public List<Channel> getByProviderId(Long providerId) {
        return channelRepository.findByProviderId(providerId);
    }

    @Override
    public List<Channel> getByProviderIdAndBillingMode(Long providerId, BillingMode billingMode) {
        return channelRepository.findByProviderIdAndBillingMode(providerId, billingMode);
    }

    /**
     * 按 ID 获取提供商（供展示组装：渠道响应需提供商名称）
     */
    @Override
    public Provider getProvider(Long providerId) {
        return providerRepository.findById(providerId).orElse(null);
    }

    /**
     * 按渠道 ID 获取端点列表（供展示组装：渠道响应需端点列表）
     */
    @Override
    public List<ChannelEndpoint> getEndpoints(Long channelId) {
        return channelEndpointRepository.findByChannelId(channelId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        channelRepository.deleteById(id);
        log.info("Deleted channel: id={}", id);
    }

    @Override
    @Transactional
    public void setState(Long id, ChannelStateCommand command) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayRequestException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));

        ChannelState currentState = channel.getState();
        ChannelState targetState = ChannelState.valueOf(command.targetState());

        // 校验状态转换合法性
        if (!currentState.canTransitionTo(targetState)) {
            throw new GatewayRequestException("INVALID_STATE_TRANSITION",
                String.format("不允许从 %s 转换为 %s", currentState, targetState));
        }

        // PENDING→ACTIVE：强制前置校验
        if (currentState == ChannelState.PENDING && targetState == ChannelState.ACTIVE) {
            validateActivationPrerequisites(channel.getId());
        }

        // PENDING→ACTIVE：级联激活 PENDING 状态的 ModelInstance
        if (currentState == ChannelState.PENDING && targetState == ChannelState.ACTIVE) {
            cascadeActivateModelInstances(channel.getId());
        }

        // SUSPENDED→ACTIVE：检查完整性（仅警告不阻塞）
        if (currentState == ChannelState.SUSPENDED && targetState == ChannelState.ACTIVE) {
            checkSuspendedActivationReadiness(channel.getId());
        }

        channel.setState(targetState);
        channelRepository.save(channel);
        log.info("渠道状态转换: id={}, {}→{}, reason={}", id, currentState, targetState, command.reason());
    }

    /**
     * 校验 PENDING→ACTIVE 前置条件
     * <p>要求至少 1 个 Endpoint + 1 个 Credential + 1 个 ModelInstance。</p>
     */
    private void validateActivationPrerequisites(Long channelId) {
        List<ChannelEndpoint> endpoints = channelEndpointRepository.findByChannelId(channelId);
        if (endpoints.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_ENDPOINT", "请先添加端点");
        }

        List<ChannelCredential> credentials = channelCredentialRepository.findByChannelId(channelId);
        if (credentials.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_CREDENTIAL", "请先添加凭证");
        }

        List<ModelInstance> instances = modelInstanceRepository.findByChannelId(channelId);
        if (instances.isEmpty()) {
            throw new GatewayRequestException("CHANNEL_NO_MODEL_INSTANCE", "请先关联模型实例");
        }
    }

    /**
     * 级联激活 PENDING 状态的 ModelInstance
     * <p>将同 Channel 下所有 PENDING 状态的 ModelInstance 设为 ACTIVE。</p>
     */
    private void cascadeActivateModelInstances(Long channelId) {
        List<ModelInstance> pendingInstances = modelInstanceRepository.findByChannelId(channelId).stream()
            .filter(mi -> mi.getState() == ModelInstance.State.PENDING)
            .toList();
        if (!pendingInstances.isEmpty()) {
            pendingInstances.forEach(mi -> mi.setState(ModelInstance.State.ACTIVE));
            modelInstanceRepository.saveAll(pendingInstances);
            log.info("级联激活 {} 个 PENDING 模型实例: channelId={}", pendingInstances.size(), channelId);
        }
    }

    /**
     * 检查 SUSPENDED→ACTIVE 完整性
     * <p>仅警告不阻塞，提示管理员检查 Endpoint/Credential/ModelInstance 是否仍完整。</p>
     */
    private void checkSuspendedActivationReadiness(Long channelId) {
        List<ChannelEndpoint> endpoints = channelEndpointRepository.findByChannelId(channelId);
        List<ChannelCredential> credentials = channelCredentialRepository.findByChannelId(channelId);
        List<ModelInstance> instances = modelInstanceRepository.findByChannelId(channelId);

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

    private void validateEndpointRequest(Long channelId, Long excludeEndpointId, ChannelEndpointCommand command) {
        if (command.getProtocol() == null || command.getProtocol().isBlank()) {
            throw new IllegalArgumentException("协议不能为空");
        }
        Protocol protocol = Protocol.fromCode(command.getProtocol());
        if (command.getEndpointUrl() == null || command.getEndpointUrl().isBlank()) {
            throw new IllegalArgumentException("端点 URL 不能为空");
        }
        String normalizedUrl = command.getEndpointUrl().trim();
        List<ChannelEndpoint> existing = channelEndpointRepository.findByChannelId(channelId);
        // 同渠道下同协议唯一（排除自身）
        if (existing.stream()
                .filter(ep -> !ep.getId().equals(excludeEndpointId))
                .anyMatch(ep -> ep.getProtocol().equals(protocol))) {
            throw new IllegalArgumentException("渠道下已存在该协议端点: " + command.getProtocol());
        }
        // 同渠道下 URL 唯一（排除自身）
        if (existing.stream()
                .filter(ep -> !ep.getId().equals(excludeEndpointId))
                .anyMatch(ep -> ep.getEndpointUrl().equals(normalizedUrl))) {
            throw new IllegalArgumentException("渠道下已存在相同 URL 的端点: " + normalizedUrl);
        }
    }

    /**
     * 添加渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpoint addEndpoint(ChannelEndpointCommand command) {
        Long channelId = command.getChannelId();
        channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("渠道不存在: " + channelId));

        validateEndpointRequest(channelId, null, command);

        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(channelId);
        endpoint.setProtocol(Protocol.fromCode(command.getProtocol()));
        endpoint.setEndpointUrl(command.getEndpointUrl().trim());

        ChannelEndpoint saved = channelEndpointRepository.save(endpoint);
        log.info("Added endpoint to channel: channelId={}, endpointId={}, protocol={}",
                channelId, saved.getId(), saved.getProtocol());
        return saved;
    }

    /**
     * 更新渠道端点
     */
    @Override
    @Transactional
    public ChannelEndpoint updateEndpoint(Long channelId, Long endpointId, ChannelEndpointCommand command) {
        ChannelEndpoint endpoint = channelEndpointRepository.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }

        validateEndpointRequest(channelId, endpointId, command);

        endpoint.setProtocol(Protocol.fromCode(command.getProtocol()));
        endpoint.setEndpointUrl(command.getEndpointUrl().trim());
        ChannelEndpoint saved = channelEndpointRepository.save(endpoint);
        log.info("Updated endpoint: channelId={}, endpointId={}", channelId, endpointId);
        return saved;
    }

    /**
     * 删除渠道端点
     */
    @Override
    @Transactional
    public void removeEndpoint(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = channelEndpointRepository.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("端点不存在: " + endpointId));
        if (!endpoint.getChannelId().equals(channelId)) {
            throw new IllegalArgumentException("端点不属于该渠道");
        }
        channelEndpointRepository.deleteById(endpointId);
        log.info("Removed endpoint: channelId={}, endpointId={}", channelId, endpointId);
    }
}