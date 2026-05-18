package com.codingas.gateway.application.gatewayapikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API Key 用量统计查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ApiKeyUsageQueryRequest {

    /** 开始时间 */
    private Instant startDate;

    /** 结束时间 */
    private Instant endDate;

    /** 用户 ID 过滤（管理员视角） */
    private Long userId;
}
