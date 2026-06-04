package com.codingas.gateway.application.catalog.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 批量物化请求
 *
 * <p>Provider 级联物化时，可选择性指定 Plans。不传 planCodes 则物化所有 ACTIVE Plans。</p>
 */
@Getter
@Setter
public class MaterializeBatchRequest {

    /** 需要物化的 Plan 编码列表（可选，不传则全部物化） */
    private List<String> planCodes;
}