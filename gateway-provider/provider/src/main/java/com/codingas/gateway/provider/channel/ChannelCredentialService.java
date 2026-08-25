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

import com.codingas.gateway.provider.channel.ApiKeyTestResponse;

import java.util.List;

/**
 * 渠道凭证应用服务接口
 */
public interface ChannelCredentialService {

    /**
     * 创建渠道凭证
     */
    ChannelCredentialCreateResponse create(ChannelCredentialCreateRequest request);

    /**
     * 根据 ID 获取渠道凭证（校验渠道归属，不含明文）
     */
    ChannelCredentialResponse getById(Long channelId, Long id);

    /**
     * 根据 ID 获取渠道凭证详情（含明文，用于页面复制）
     */
    ChannelCredentialDetailResponse getDetailById(Long channelId, Long id);

    /**
     * 获取渠道下的所有凭证
     */
    List<ChannelCredentialResponse> listByChannelId(Long channelId);

    /**
     * 更新渠道凭证（校验渠道归属）
     */
    ChannelCredentialResponse update(ChannelCredentialUpdateRequest request);

    /**
     * 删除渠道凭证（校验渠道归属）
     */
    void delete(Long channelId, Long id);

    /**
     * 测试 API Key 是否有效
     */
    ApiKeyTestResponse testApiKey(Long channelId, Long id);
}
