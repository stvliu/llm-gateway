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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.channel.Channel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 渠道复制请求 DTO（HTTP 契约）
 *
 * <p>仅承载覆盖字段（name 必填）；其余配置（供应商/计费/配额/超时/重试）、
 * 端点与模型实例由服务层从源渠道复制。凭证（API Key）复制由
 * {@code copyCredentials} 控制，默认不复制。</p>
 */
@Data
public class ChannelCopyRequest {

    /** 新渠道名称（必填，同一供应商下唯一） */
    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 是否同时复制凭证（API Key），默认 false */
    private boolean copyCredentials = false;

    /**
     * 转换为覆盖字段实体（其余配置由服务层从源渠道复制）
     *
     * @return 仅含覆盖字段的 Channel
     */
    public Channel toEntity() {
        Channel channel = new Channel();
        channel.setName(name);
        return channel;
    }
}
