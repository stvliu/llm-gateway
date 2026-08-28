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
package com.codingas.gateway.common.util;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * 列表查询排序助手
 *
 * <p>统一提供排序方向解析、排序字段白名单归一化与比较器构建，
 * 供各域查询服务（Model/Channel/ModelInstance 等）复用，避免重复实现。</p>
 */
public final class SortSupport {

    private SortSupport() {}

    /**
     * 归一化排序字段
     *
     * <p>仅允许白名单内的字段，其余（含 null/空白/注入尝试）回退默认字段，防止
     * 通过查询参数注入任意排序字段。</p>
     *
     * @param sortBy      请求排序字段
     * @param allowed     可排序字段白名单
     * @param defaultSort 默认排序字段
     * @return 归一化后的排序字段
     */
    public static String normalize(String sortBy, Set<String> allowed, String defaultSort) {
        if (sortBy == null || sortBy.isBlank()) {
            return defaultSort;
        }
        return allowed.contains(sortBy) ? sortBy : defaultSort;
    }

    /**
     * 解析排序方向：仅识别 DESC（忽略大小写），其余视为升序
     *
     * @param sortOrder 请求排序方向
     * @return true=降序
     */
    public static boolean isDesc(String sortOrder) {
        return "DESC".equalsIgnoreCase(sortOrder);
    }

    /**
     * 字符串字段比较器（大小写不敏感字母序）
     *
     * <p>升序时 null 值垫底；降序经整体反转后 null 值置顶（与 Postgres DESC 默认
     * NULLS FIRST 行为一致）。</p>
     *
     * @param getter 字段提取器
     * @param desc   是否降序
     * @return 比较器
     */
    public static <T> Comparator<T> byString(Function<T, String> getter, boolean desc) {
        Comparator<T> comparator = (a, b) -> {
            String va = getter.apply(a);
            String vb = getter.apply(b);
            if (va == null) return vb == null ? 0 : 1;
            if (vb == null) return -1;
            return String.CASE_INSENSITIVE_ORDER.compare(va, vb);
        };
        return desc ? comparator.reversed() : comparator;
    }

    /**
     * 长整型字段比较器
     *
     * @param getter 字段提取器
     * @param desc   是否降序
     * @return 比较器
     */
    public static <T> Comparator<T> byLong(ToLongFunction<T> getter, boolean desc) {
        Comparator<T> comparator = Comparator.comparingLong(getter);
        return desc ? comparator.reversed() : comparator;
    }
}
