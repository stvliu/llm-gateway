package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 物化结果
 */
@Getter
@Builder
public class MaterializeResult {

    /** 物化类型：PROVIDER / PLAN / MODEL */
    private final String type;

    /** 业务编码 */
    private final String code;

    /** 物化后运营实体 ID */
    private final Long entityId;

    /** 结果状态：CREATED / SKIPPED */
    private final String status;
}
