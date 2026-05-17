package com.codingas.gateway.application.gatewayapikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API Key 用量统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyUsageResponse {

    /** API Key ID */
    private Long apiKeyId;

    /** API Key 名称 */
    private String apiKeyName;

    /** 总调用次数 */
    private long totalCalls;

    /** 输入 Token 总量 */
    private long totalInputTokens;

    /** 输出 Token 总量 */
    private long totalOutputTokens;

    /** Token 总量 */
    private long totalTokens;

    /** 统计开始时间 */
    private Instant startDate;

    /** 统计结束时间 */
    private Instant endDate;
}
