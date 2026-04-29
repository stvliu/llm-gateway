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
