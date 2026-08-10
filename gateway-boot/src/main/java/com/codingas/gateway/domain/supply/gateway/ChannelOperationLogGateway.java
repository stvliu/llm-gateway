/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;

import java.util.List;
import java.util.Optional;

/**
 * 渠道操作日志 Gateway 接口
 *
 * <p>定义操作日志的持久化契约，由 infrastructure 层实现。</p>
 */
public interface ChannelOperationLogGateway {

    /**
     * 保存操作日志
     */
    void save(ChannelOperationLog log);

    /**
     * 批量保存操作日志
     */
    void saveAll(List<ChannelOperationLog> logs);

    /**
     * 根据 ID 查询操作日志
     */
    Optional<ChannelOperationLog> findById(Long id);

    /**
     * 查询指定渠道的操作日志（按时间倒序）
     *
     * @param channelId 渠道 ID
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 操作日志列表
     */
    List<ChannelOperationLog> findByChannelId(Long channelId, int page, int size);

    /**
     * 查询指定渠道的操作日志（按操作类型过滤）
     *
     * @param channelId 渠道 ID
     * @param actions   操作类型列表
     * @param page      页码（从 0 开始）
     * @param size      每页大小
     * @return 操作日志列表
     */
    List<ChannelOperationLog> findByChannelIdAndActions(Long channelId, List<String> actions, int page, int size);

    /**
     * 统计指定渠道的操作日志总数
     */
    long countByChannelId(Long channelId);

    /**
     * 查询指定操作人的操作日志（全局）
     */
    List<ChannelOperationLog> findByOperatorId(Long operatorId, int page, int size);

    /**
     * 查询指定批量操作 ID 的所有日志
     */
    List<ChannelOperationLog> findByBatchId(Long batchId);
}
