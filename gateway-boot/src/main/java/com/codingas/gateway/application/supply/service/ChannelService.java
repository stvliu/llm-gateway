package com.codingas.gateway.application.supply.service;

import com.codingas.gateway.domain.supply.entity.*;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.exception.ChannelNotFoundException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelOperationLogGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 渠道管理应用服务
 *
 * <p>编排渠道的 CRUD、启停、复制等操作，并在每次操作后记录操作日志。</p>
 *
 * @deprecated 使用 {@link com.codingas.gateway.application.channel.ChannelServiceImpl} 替代
 */
@Deprecated
@Service
public class ChannelService {

    private final ChannelGateway channelGateway;
    private final ChannelOperationLogGateway operationLogGateway;

    public ChannelService(ChannelGateway channelGateway,
                          ChannelOperationLogGateway operationLogGateway) {
        this.channelGateway = channelGateway;
        this.operationLogGateway = operationLogGateway;
    }

    /**
     * 编辑渠道
     *
     * @param channelId     渠道 ID
     * @param updateReq     更新请求
     * @param operatorId    操作人 ID
     * @param operatorName  操作人姓名
     * @param operatorIp    操作人 IP
     * @return 更新后的渠道
     */
    @Transactional
    public Channel update(Long channelId, Channel updateReq,
                          Long operatorId, String operatorName, String operatorIp) {
        Channel existing = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        // 记录变更详情
        String changeDetail = buildChangeDetail(existing, updateReq);

        // 更新字段
        if (updateReq.getName() != null) existing.setName(updateReq.getName());
        if (updateReq.getProviderId() != null) existing.setProviderId(updateReq.getProviderId());
        if (updateReq.getProviderName() != null) existing.setProviderName(updateReq.getProviderName());
        if (updateReq.getApiEndpoint() != null) existing.setApiEndpoint(updateReq.getApiEndpoint());
        if (updateReq.getApiKey() != null) existing.setApiKey(updateReq.getApiKey());
        if (updateReq.getProtocolType() != null) existing.setProtocolType(updateReq.getProtocolType());
        if (updateReq.getWeight() != null) existing.setWeight(updateReq.getWeight());
        if (updateReq.getTimeout() != null) existing.setTimeout(updateReq.getTimeout());
        if (updateReq.getMaxRetries() != null) existing.setMaxRetries(updateReq.getMaxRetries());
        existing.setUpdatedBy(operatorId);
        existing.setUpdatedAt(LocalDateTime.now());

        Channel saved = channelGateway.save(existing);

        // 记录操作日志
        saveOperationLog(channelId, existing.getName(), ChannelActions.UPDATE,
                changeDetail, operatorId, operatorName, operatorIp, null);

        return saved;
    }

    /**
     * 启用渠道
     */
    @Transactional
    public Channel enable(Long channelId, Long operatorId, String operatorName, String operatorIp) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (channel.getState() == ChannelState.ACTIVE) {
            return channel; // 已经是启用状态，直接返回
        }

        ChannelState oldState = channel.getState();
        channel.setState(ChannelState.ACTIVE);
        channel.setUpdatedBy(operatorId);
        channel.setUpdatedAt(LocalDateTime.now());

        Channel saved = channelGateway.save(channel);

        String changeDetail = String.format(
                "{\"changedFields\":[{\"field\":\"status\",\"label\":\"渠道状态\",\"from\":\"%s\",\"to\":\"ACTIVE\"}]}",
                oldState.name());

        saveOperationLog(channelId, channel.getName(), ChannelActions.ENABLE,
                changeDetail, operatorId, operatorName, operatorIp, null);

        return saved;
    }

    /**
     * 停用渠道
     */
    @Transactional
    public Channel disable(Long channelId, Long operatorId, String operatorName, String operatorIp) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        if (channel.getState() == ChannelState.SUSPENDED) {
            return channel;
        }

        ChannelState oldState = channel.getState();
        channel.setState(ChannelState.SUSPENDED);
        channel.setUpdatedBy(operatorId);
        channel.setUpdatedAt(LocalDateTime.now());

        Channel saved = channelGateway.save(channel);

        String changeDetail = String.format(
                "{\"changedFields\":[{\"field\":\"status\",\"label\":\"渠道状态\",\"from\":\"%s\",\"to\":\"SUSPENDED\"}]}",
                oldState.name());

        saveOperationLog(channelId, channel.getName(), ChannelActions.DISABLE,
                changeDetail, operatorId, operatorName, operatorIp, null);

        return saved;
    }

    /**
     * 复制渠道
     *
     * <p>基于现有渠道创建一个新的草稿渠道，名称自动添加"(副本)"后缀，
     * 权重重置为默认值，状态强制设为草稿。</p>
     */
    @Transactional
    public Channel copy(Long channelId, Long operatorId, String operatorName, String operatorIp) {
        Channel source = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        Channel copy = new Channel();
        copy.setName(source.getName() + " (副本)");
        copy.setProviderId(source.getProviderId());
        copy.setProviderName(source.getProviderName());
        copy.setApiEndpoint(source.getApiEndpoint());
        copy.setApiKey(source.getApiKey());
        copy.setProtocolType(source.getProtocolType());
        copy.setWeight(50); // 权重重置为默认值
        copy.setTimeout(source.getTimeout());
        copy.setMaxRetries(source.getMaxRetries());
        copy.setState(ChannelState.PENDING);
        copy.setCreatedBy(operatorId);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedBy(operatorId);
        copy.setUpdatedAt(LocalDateTime.now());

        Channel saved = channelGateway.save(copy);

        String changeDetail = String.format(
                "{\"sourceChannelId\":%d,\"sourceChannelName\":\"%s\"}",
                source.getId(), source.getName());

        saveOperationLog(saved.getId(), saved.getName(), ChannelActions.COPY,
                changeDetail, operatorId, operatorName, operatorIp, null);

        return saved;
    }

    /**
     * 删除渠道（逻辑删除）
     */
    @Transactional
    public void delete(Long channelId, Long operatorId, String operatorName, String operatorIp) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        String channelName = channel.getName(); // 删除前记录名称快照
        channel.setState(ChannelState.RETIRED);
        channel.setUpdatedBy(operatorId);
        channel.setUpdatedAt(LocalDateTime.now());

        channelGateway.save(channel);

        saveOperationLog(channelId, channelName, ChannelActions.DELETE,
                "{}", operatorId, operatorName, operatorIp, null);
    }

    // ========== 内部方法 ==========

    /**
     * 构建变更详情 JSON
     */
    private String buildChangeDetail(Channel before, Channel after) {
        StringBuilder sb = new StringBuilder("{\"changedFields\":[");
        boolean first = true;

        if (after.getName() != null && !after.getName().equals(before.getName())) {
            appendField(sb, first, "name", "渠道名称", before.getName(), after.getName());
            first = false;
        }
        if (after.getApiEndpoint() != null && !after.getApiEndpoint().equals(before.getApiEndpoint())) {
            appendField(sb, first, "apiEndpoint", "API 地址", before.getApiEndpoint(), after.getApiEndpoint());
            first = false;
        }
        if (after.getApiKey() != null) {
            appendField(sb, first, "apiKey", "API Key", "***", "***");
            first = false;
        }
        if (after.getWeight() != null && !after.getWeight().equals(before.getWeight())) {
            appendField(sb, first, "weight", "权重",
                    String.valueOf(before.getWeight()), String.valueOf(after.getWeight()));
            first = false;
        }
        if (after.getTimeout() != null && !after.getTimeout().equals(before.getTimeout())) {
            appendField(sb, first, "timeout", "超时时间",
                    String.valueOf(before.getTimeout()), String.valueOf(after.getTimeout()));
            first = false;
        }
        if (after.getMaxRetries() != null && !after.getMaxRetries().equals(before.getMaxRetries())) {
            appendField(sb, first, "maxRetries", "最大重试",
                    String.valueOf(before.getMaxRetries()), String.valueOf(after.getMaxRetries()));
        }

        sb.append("]}");
        return sb.toString();
    }

    private void appendField(StringBuilder sb, boolean first, String field, String label,
                             String from, String to) {
        if (!first) sb.append(",");
        sb.append(String.format(
                "{\"field\":\"%s\",\"label\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}",
                field, label, escapeJson(from), escapeJson(to)));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(Long channelId, String channelName, String action,
                                  String changeDetail, Long operatorId, String operatorName,
                                  String operatorIp, Long batchId) {
        ChannelOperationLog log = new ChannelOperationLog();
        log.setChannelId(channelId);
        log.setChannelName(channelName);
        log.setAction(action);
        log.setActionLabel(ChannelActions.getLabel(action));
        log.setChangeDetail(changeDetail);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorIp(operatorIp);
        log.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        log.setOperatedAt(LocalDateTime.now());
        log.setBatchId(batchId);
        operationLogGateway.save(log);
    }
}
