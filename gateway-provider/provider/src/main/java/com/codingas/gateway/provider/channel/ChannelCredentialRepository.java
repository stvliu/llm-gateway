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


import java.util.List;
import java.util.Optional;

/**
 * 渠道凭证持久化接口
 */
public interface ChannelCredentialRepository {

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
     * 查找渠道的默认凭证
     */
    Optional<ChannelCredential> findDefaultByChannelId(Long channelId);

    /**
     * 删除凭证
     */
    void deleteById(Long id);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}