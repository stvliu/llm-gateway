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

/**
 * 渠道操作动作常量定义
 *
 * <p>定义所有可记录的渠道操作类型，按严重级别分组：</p>
 * <ul>
 *   <li>CRITICAL：创建、删除</li>
 *   <li>WARNING：启用、停用、批量操作</li>
 *   <li>INFO：编辑配置、测试、复制</li>
 * </ul>
 */
public final class ChannelActions {

    private ChannelActions() {}

    /** 创建渠道 */
    public static final String CREATE = "CREATE";
    /** 编辑渠道配置 */
    public static final String UPDATE = "UPDATE";
    /** 启用渠道 */
    public static final String ENABLE = "ENABLE";
    /** 停用渠道 */
    public static final String DISABLE = "DISABLE";
    /** 测试连通性 */
    public static final String TEST = "TEST";
    /** 复制渠道 */
    public static final String COPY = "COPY";
    /** 删除渠道 */
    public static final String DELETE = "DELETE";
    /** 废弃渠道（仍可路由） */
    public static final String DEPRECATE = "DEPRECATE";
    /** 退役渠道（终态） */
    public static final String RETIRE = "RETIRE";
    /** 批量启用 */
    public static final String BATCH_ENABLE = "BATCH_ENABLE";
    /** 批量停用 */
    public static final String BATCH_DISABLE = "BATCH_DISABLE";
    /** 批量删除 */
    public static final String BATCH_DELETE = "BATCH_DELETE";

    /** 操作级别：CRITICAL */
    public static final String LEVEL_CRITICAL = "CRITICAL";
    /** 操作级别：WARNING */
    public static final String LEVEL_WARNING = "WARNING";
    /** 操作级别：INFO */
    public static final String LEVEL_INFO = "INFO";

    /**
     * 获取操作的中文标签
     */
    public static String getLabel(String action) {
        return switch (action) {
            case CREATE -> "新建渠道";
            case UPDATE -> "编辑渠道";
            case ENABLE -> "启用渠道";
            case DISABLE -> "停用渠道";
            case TEST -> "测试连通性";
            case COPY -> "复制渠道";
            case DELETE -> "删除渠道";
            case DEPRECATE -> "废弃渠道";
            case RETIRE -> "退役渠道";
            case BATCH_ENABLE -> "批量启用";
            case BATCH_DISABLE -> "批量停用";
            case BATCH_DELETE -> "批量删除";
            default -> "未知操作";
        };
    }

    /**
     * 获取操作的严重级别
     */
    public static String getLevel(String action) {
        return switch (action) {
            case CREATE, DELETE, BATCH_DELETE -> LEVEL_CRITICAL;
            case ENABLE, DISABLE, BATCH_ENABLE, BATCH_DISABLE -> LEVEL_WARNING;
            case UPDATE, TEST, COPY -> LEVEL_INFO;
            default -> LEVEL_INFO;
        };
    }
}
