package com.codingas.gateway.application.resilience.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 容灾画像响应 DTO
 *
 * <p>返回容灾画像聚合根的完整字段，含主键与审计字段。</p>
 */
@Data
public class ResilienceProfileResponse {

    /** 画像 ID */
    private Long id;

    /** 画像编码，全局唯一 */
    private String code;

    /** 画像名称 */
    private String name;

    /** 容灾模式档位（STANDARD/STRICT/AGGRESSIVE） */
    private String mode;

    /** 是否启用 L2 模型级降级兜底 */
    private boolean enableL2ModelDegradation;

    /** L2 降级最大深度（0 表示禁用降级） */
    private int degradationMaxDepth;

    /** 是否启用会话亲和 */
    private boolean enableSessionAffinity;

    /** 会话亲和 TTL（分钟） */
    private int sessionAffinityTtlMinutes;

    /** 是否启用模型锁定 */
    private boolean enablePinnedModel;

    /** 锁定模型 ID（可空） */
    private Long pinnedModelId;

    /** 请求超时秒数（0 表示用渠道默认） */
    private int timeout;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
