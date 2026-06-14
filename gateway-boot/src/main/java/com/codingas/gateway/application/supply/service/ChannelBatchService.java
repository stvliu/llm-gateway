package com.codingas.gateway.application.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelActions;
import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.exception.ChannelNotFoundException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelOperationLogGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 渠道批量操作应用服务
 *
 * <p>处理批量启用、停用、删除操作，逐项执行并汇总结果。</p>
 *
 * @deprecated 使用 {@link com.codingas.gateway.application.channel.ChannelServiceImpl} 替代
 */
@Deprecated
@Service
public class ChannelBatchService {

    private final ChannelGateway channelGateway;
    private final ChannelOperationLogGateway operationLogGateway;

    public ChannelBatchService(ChannelGateway channelGateway,
                               ChannelOperationLogGateway operationLogGateway) {
        this.channelGateway = channelGateway;
        this.operationLogGateway = operationLogGateway;
    }

    /**
     * 批量操作结果
     */
    public record BatchResult(
            int total,
            int succeeded,
            int failed,
            List<BatchItemResult> results
    ) {}

    /**
     * 单条操作结果
     */
    public record BatchItemResult(
            Long channelId,
            String channelName,
            boolean success,
            String reason
    ) {}

    /**
     * 批量启用渠道
     *
     * <p>仅对停用状态的渠道生效，其他状态自动跳过。</p>
     */
    @Transactional
    public BatchResult batchEnable(List<Long> ids, Long operatorId, String operatorName, String operatorIp) {
        Long batchId = System.currentTimeMillis();
        List<BatchItemResult> results = new ArrayList<>();
        List<ChannelOperationLog> logs = new ArrayList<>();

        for (Long id : ids) {
            try {
                Channel channel = channelGateway.findById(id)
                        .orElseThrow(() -> new ChannelNotFoundException(id));

                if (channel.getState() != ChannelState.SUSPENDED) {
                    results.add(new BatchItemResult(id, channel.getName(), false, "渠道不在停用状态"));
                    continue;
                }

                ChannelState oldState = channel.getState();
                channel.setState(ChannelState.ACTIVE);
                channel.setUpdatedBy(operatorId);
                channel.setUpdatedAt(LocalDateTime.now());
                channelGateway.save(channel);

                results.add(new BatchItemResult(id, channel.getName(), true, null));

                // 构建操作日志
                String changeDetail = String.format(
                        "{\"changedFields\":[{\"field\":\"status\",\"label\":\"渠道状态\",\"from\":\"%s\",\"to\":\"ACTIVE\"}]}",
                        oldState.name());
                logs.add(buildLog(id, channel.getName(), ChannelActions.BATCH_ENABLE,
                        changeDetail, operatorId, operatorName, operatorIp, batchId));

            } catch (Exception e) {
                results.add(new BatchItemResult(id, null, false, e.getMessage()));
            }
        }

        if (!logs.isEmpty()) {
            operationLogGateway.saveAll(logs);
        }

        long succeeded = results.stream().filter(BatchItemResult::success).count();
        return new BatchResult(ids.size(), (int) succeeded, ids.size() - (int) succeeded, results);
    }

    /**
     * 批量停用渠道
     *
     * <p>仅对启用状态的渠道生效，其他状态自动跳过。</p>
     */
    @Transactional
    public BatchResult batchDisable(List<Long> ids, Long operatorId, String operatorName, String operatorIp) {
        Long batchId = System.currentTimeMillis();
        List<BatchItemResult> results = new ArrayList<>();
        List<ChannelOperationLog> logs = new ArrayList<>();

        for (Long id : ids) {
            try {
                Channel channel = channelGateway.findById(id)
                        .orElseThrow(() -> new ChannelNotFoundException(id));

                if (channel.getState() != ChannelState.ACTIVE) {
                    results.add(new BatchItemResult(id, channel.getName(), false, "渠道不在启用状态"));
                    continue;
                }

                ChannelState oldState = channel.getState();
                channel.setState(ChannelState.SUSPENDED);
                channel.setUpdatedBy(operatorId);
                channel.setUpdatedAt(LocalDateTime.now());
                channelGateway.save(channel);

                results.add(new BatchItemResult(id, channel.getName(), true, null));

                String changeDetail = String.format(
                        "{\"changedFields\":[{\"field\":\"status\",\"label\":\"渠道状态\",\"from\":\"%s\",\"to\":\"SUSPENDED\"}]}",
                        oldState.name());
                logs.add(buildLog(id, channel.getName(), ChannelActions.BATCH_DISABLE,
                        changeDetail, operatorId, operatorName, operatorIp, batchId));

            } catch (Exception e) {
                results.add(new BatchItemResult(id, null, false, e.getMessage()));
            }
        }

        if (!logs.isEmpty()) {
            operationLogGateway.saveAll(logs);
        }

        long succeeded = results.stream().filter(BatchItemResult::success).count();
        return new BatchResult(ids.size(), (int) succeeded, ids.size() - (int) succeeded, results);
    }

    /**
     * 批量删除渠道
     */
    @Transactional
    public BatchResult batchDelete(List<Long> ids, Long operatorId, String operatorName, String operatorIp) {
        Long batchId = System.currentTimeMillis();
        List<BatchItemResult> results = new ArrayList<>();
        List<ChannelOperationLog> logs = new ArrayList<>();

        for (Long id : ids) {
            try {
                Channel channel = channelGateway.findById(id)
                        .orElseThrow(() -> new ChannelNotFoundException(id));

                String channelName = channel.getName();
                channel.setState(ChannelState.RETIRED);
                channel.setUpdatedBy(operatorId);
                channel.setUpdatedAt(LocalDateTime.now());
                channelGateway.save(channel);

                results.add(new BatchItemResult(id, channelName, true, null));
                logs.add(buildLog(id, channelName, ChannelActions.BATCH_DELETE,
                        "{}", operatorId, operatorName, operatorIp, batchId));

            } catch (Exception e) {
                results.add(new BatchItemResult(id, null, false, e.getMessage()));
            }
        }

        if (!logs.isEmpty()) {
            operationLogGateway.saveAll(logs);
        }

        long succeeded = results.stream().filter(BatchItemResult::success).count();
        return new BatchResult(ids.size(), (int) succeeded, ids.size() - (int) succeeded, results);
    }

    private ChannelOperationLog buildLog(Long channelId, String channelName, String action,
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
        return log;
    }
}
