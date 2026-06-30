package com.codingas.gateway.domain.resilience.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 容灾画像聚合根实体
 *
 * <p>容灾画像（ResilienceProfile）是应用级容灾配置的载体，承载四层容灾栈
 * （L0 Key 级 / L1 Channel 级 / L2 模型级 / L3 抛错）的开关与参数。
 * 画像全落库 + CRUD，预设档位（default/strict/aggressive/batch）由初始化数据写入，
 * 解析链 Application → Global（见 design.md D5）。</p>
 *
 * <p>领域模型纯洁：仅含 Getter/Setter，不含业务逻辑；
 * 档位→字段推导由 {@code ResilienceResolver}（后续任务）完成。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>code — 画像编码，全局唯一（如 'default' / 'claude-code' / 'helpdesk'）</li>
 *   <li>name — 画像名称</li>
 *   <li>mode — 容灾模式档位（STANDARD/STRICT/AGGRESSIVE），管理员面向字段</li>
 *   <li>enableL2ModelDegradation — 是否启用 L2 模型级降级兜底</li>
 *   <li>degradationMaxDepth — L2 降级最大深度（0 表示禁用降级）</li>
 *   <li>timeout — 请求超时秒数（0 表示用渠道默认）</li>
 *   <li>id, createdBy, createdAt, updatedBy, updatedAt — 主键与审计字段，继承自 {@link BaseEntity}</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DomainEntity
public class ResilienceProfile extends BaseEntity {

    /** 画像编码，全局唯一 */
    private String code;

    /** 画像名称 */
    private String name;

    /** 容灾模式档位 */
    private ResilienceMode mode;

    /** 是否启用 L2 模型级降级兜底 */
    private boolean enableL2ModelDegradation;

    /** L2 降级最大深度（0 表示禁用降级） */
    private int degradationMaxDepth;

    /** 请求超时秒数（0 表示用渠道默认） */
    private int timeout;
}
