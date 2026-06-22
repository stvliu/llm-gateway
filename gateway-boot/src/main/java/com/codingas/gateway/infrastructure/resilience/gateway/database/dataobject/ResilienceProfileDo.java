package com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 容灾画像数据对象
 *
 * <p>对应 resilience_profiles 表；主键与审计字段（created_by/created_at/updated_by/updated_at）
 * 继承自 {@link BaseDo}，由 AuditingEntityListener 自动填充。</p>
 *
 * <p>mode 字段以字符串存储（STANDARD/STRICT/AGGRESSIVE），由 Gateway 实现层
 * 在 DO↔Entity 转换时还原为 {@link com.codingas.gateway.domain.resilience.entity.ResilienceMode} 枚举。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "resilience_profiles")
public class ResilienceProfileDo extends BaseDo {

    /** 画像编码，全局唯一 */
    @Column(name = "code", nullable = false, length = 64, unique = true)
    private String code;

    /** 画像名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 容灾模式档位（STANDARD/STRICT/AGGRESSIVE） */
    @Column(name = "mode", nullable = false, length = 16)
    private String mode;

    /** 是否启用 L2 模型级降级兜底 */
    @Column(name = "enable_l2_model_degradation", nullable = false)
    private boolean enableL2ModelDegradation;

    /** L2 降级最大深度（0 表示禁用降级） */
    @Column(name = "degradation_max_depth", nullable = false)
    private int degradationMaxDepth;

    /** 是否启用会话亲和 */
    @Column(name = "enable_session_affinity", nullable = false)
    private boolean enableSessionAffinity;

    /** 会话亲和 TTL（分钟） */
    @Column(name = "session_affinity_ttl_minutes", nullable = false)
    private int sessionAffinityTtlMinutes;

    /** 是否启用模型锁定 */
    @Column(name = "enable_pinned_model", nullable = false)
    private boolean enablePinnedModel;

    /** 锁定模型 ID（可空） */
    @Column(name = "pinned_model_id")
    private Long pinnedModelId;

    /** 请求超时秒数（0 表示用渠道默认） */
    @Column(name = "timeout", nullable = false)
    private int timeout;
}
