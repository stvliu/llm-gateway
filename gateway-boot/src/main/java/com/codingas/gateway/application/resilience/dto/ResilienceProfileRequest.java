package com.codingas.gateway.application.resilience.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 容灾画像创建/更新请求 DTO
 *
 * <p>承载容灾画像聚合根可编辑字段。code 全局唯一；mode 为管理员面向档位，
 * 其余字段为四层容灾栈（L0/L1/L2/L3）的开关与参数。</p>
 *
 * <p>不提供 delete 端点：default 画像为系统兜底，禁止删除；其余画像
 * 因 {@code ResilienceProfileGateway} 无 delete 方法，遵循既有模式不新增。</p>
 */
@Data
public class ResilienceProfileRequest {

    /** 画像编码，全局唯一 */
    @NotBlank(message = "画像编码不能为空")
    private String code;

    /** 画像名称 */
    @NotBlank(message = "画像名称不能为空")
    private String name;

    /** 容灾模式档位（STANDARD/STRICT/AGGRESSIVE） */
    @NotBlank(message = "容灾模式不能为空")
    private String mode;

    /** 是否启用 L2 模型级降级兜底 */
    private boolean enableL2ModelDegradation;

    /** L2 降级最大深度（0 表示禁用降级） */
    @Min(value = 0, message = "降级深度不能为负数")
    private int degradationMaxDepth;

    /** 是否启用会话亲和 */
    private boolean enableSessionAffinity;

    /** 会话亲和 TTL（分钟） */
    @Min(value = 0, message = "会话亲和 TTL 不能为负数")
    private int sessionAffinityTtlMinutes;

    /** 是否启用模型锁定 */
    private boolean enablePinnedModel;

    /** 锁定模型 ID（可空） */
    private Long pinnedModelId;

    /** 请求超时秒数（0 表示用渠道默认） */
    @Min(value = 0, message = "超时秒数不能为负数")
    private int timeout;
}
