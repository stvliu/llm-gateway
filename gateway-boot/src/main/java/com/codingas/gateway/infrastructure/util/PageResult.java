package com.codingas.gateway.infrastructure.util;

import java.util.List;

/**
 * 分页结果封装
 *
 * @param <T> 元素类型
 */
public record PageResult<T>(
    List<T> items,
    long total,
    int page,
    int size,
    int totalPages
) {
    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(items, total, page, size, totalPages);
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }
}
