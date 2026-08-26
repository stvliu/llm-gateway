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
package com.codingas.gateway.provider.vendor;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 供应商实体
 *
 * <p>表示 AI 模型服务提供商，如 OpenAI、Anthropic、智谱等。</p>
 * <p>品牌标识使用 name 字段，code 字段为程序标识。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Provider extends BaseEntity {

    /** 程序标识（如 "openai", "anthropic", "zhipu"） */
    private String code;

    /** 显示名（如 "OpenAI", "智谱AI"） */
    private String name;

    private String logoUrl;

    private String websiteUrl;

    private String description;

    /** API 文档 URL */
    private String apiDocUrl;

    /** 路由优先级（数值越小优先级越高） */
    private Integer priority;

    

}