/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;

import java.util.List;
import java.util.Optional;

/**
 * 渠道凭证持久化接口
 */
public interface ChannelCredentialGateway {

    /**
     * 保存凭证
     */
    ChannelCredential save(ChannelCredential credential);

    /**
     * 根据 ID 查找凭证
     */
    Optional<ChannelCredential> findById(Long id);

    /**
     * 根据渠道 ID 查找凭证
     */
    List<ChannelCredential> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 查找活跃凭证
     */
    List<ChannelCredential> findActiveByChannelId(Long channelId);

    /**
     * 根据渠道 ID 和状态查找凭证
     */
    List<ChannelCredential> findByChannelIdAndState(Long channelId, String state);

    /**
     * 查找渠道的默认凭证
     */
    Optional<ChannelCredential> findDefaultByChannelId(Long channelId);

    /**
     * 更新最后使用时间
     */
    void updateLastUsedAt(Long id);

    /**
     * 删除凭证
     */
    void deleteById(Long id);

    /**
     * 统计渠道活跃凭证数
     */
    long countActiveByChannelId(Long channelId);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}