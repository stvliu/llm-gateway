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
package com.codingas.gateway.common.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> items;
    private Pagination pagination;

    @Data
    public static class Pagination {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int limit, long total) {
        PageResponse<T> response = new PageResponse<>();
        response.setItems(items);
        Pagination pagination = new Pagination();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotal(total);
        pagination.setTotalPages((int) Math.ceil((double) total / limit));
        response.setPagination(pagination);
        return response;
    }
}
