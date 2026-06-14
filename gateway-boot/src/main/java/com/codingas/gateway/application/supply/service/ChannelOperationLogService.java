package com.codingas.gateway.application.supply.service;

import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import com.codingas.gateway.domain.supply.gateway.ChannelOperationLogGateway;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道操作日志应用服务
 */
@Service
public class ChannelOperationLogService {

    private final ChannelOperationLogGateway operationLogGateway;

    public ChannelOperationLogService(ChannelOperationLogGateway operationLogGateway) {
        this.operationLogGateway = operationLogGateway;
    }

    /**
     * 查询指定渠道的操作日志
     *
     * @param channelId 渠道 ID
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 操作日志列表
     */
    public List<ChannelOperationLog> listByChannel(Long channelId, int page, int size) {
        return operationLogGateway.findByChannelId(channelId, page, size);
    }

    /**
     * 查询指定渠道的过滤操作日志
     */
    public List<ChannelOperationLog> listByChannelAndActions(Long channelId, List<String> actions, int page, int size) {
        return operationLogGateway.findByChannelIdAndActions(channelId, actions, page, size);
    }

    /**
     * 统计指定渠道的操作日志总数
     */
    public long countByChannel(Long channelId) {
        return operationLogGateway.countByChannelId(channelId);
    }

    /**
     * 查询全局操作日志
     */
    public List<ChannelOperationLog> listByOperator(Long operatorId, int page, int size) {
        return operationLogGateway.findByOperatorId(operatorId, page, size);
    }

    /**
     * 查询指定批量操作的日志
     */
    public List<ChannelOperationLog> listByBatch(Long batchId) {
        return operationLogGateway.findByBatchId(batchId);
    }
}
