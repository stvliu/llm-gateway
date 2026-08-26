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

/**
 * 渠道凭证应用服务接口
 *
 * <p>出入参采用领域实体与轻量用例对象，HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface ChannelCredentialManager {

    /**
     * 创建渠道凭证
     *
     * @param command 创建用例入参
     * @return 创建后的凭证实体（含仅此一次可见的明文 apiKeyPlain）
     */
    ChannelCredential create(ChannelCredentialCreateCommand command);

    /**
     * 根据 ID 获取渠道凭证（校验渠道归属，不含明文）
     *
     * @param channelId 渠道 ID
     * @param id        凭证 ID
     * @return 凭证实体
     */
    ChannelCredential getById(Long channelId, Long id);

    /**
     * 根据 ID 获取渠道凭证详情（含明文，用于页面复制）
     *
     * @param channelId 渠道 ID
     * @param id        凭证 ID
     * @return 凭证实体
     */
    ChannelCredential getDetailById(Long channelId, Long id);

    /**
     * 获取渠道下的所有凭证
     *
     * @param channelId 渠道 ID
     * @return 凭证实体列表
     */
    List<ChannelCredential> listByChannelId(Long channelId);

    /**
     * 更新渠道凭证（校验渠道归属）
     *
     * @param command 更新用例入参
     * @return 更新后的凭证实体
     */
    ChannelCredential update(ChannelCredentialUpdateCommand command);

    /**
     * 删除渠道凭证（校验渠道归属）
     *
     * @param channelId 渠道 ID
     * @param id        凭证 ID
     */
    void delete(Long channelId, Long id);

    /**
     * 测试 API Key 是否有效
     *
     * @param channelId 渠道 ID
     * @param id        凭证 ID
     * @return 测试用例结果
     */
    ApiKeyTestResult testApiKey(Long channelId, Long id);
}
