package com.codingas.gateway.application.proxy.failover;

import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 错误分流表 (D3)
 *
 * <p>按 {@link ProviderErrorType} 映射到 {@link FailoverDecision}，
 * 指导故障转移层级选择（换渠道 L1 / 换模型 L2 / 不转移 NONE）。</p>
 *
 * <p>分流规则：</p>
 * <ul>
 *   <li>{@link ProviderErrorType#INVALID_REQUEST} → {@link FailoverDecision#NONE}
 *       （请求级错误，换哪都无效，直接抛出）</li>
 *   <li>共因故障（{@link ProviderErrorType#AUTHENTICATION_ERROR}/{@link ProviderErrorType#RATE_LIMIT_ERROR}/
 *       {@link ProviderErrorType#QUOTA_EXCEEDED}/{@link ProviderErrorType#TIMEOUT_ERROR}/
 *       {@link ProviderErrorType#UPSTREAM_ERROR}/{@link ProviderErrorType#SERVICE_UNAVAILABLE}/
 *       {@link ProviderErrorType#NETWORK_ERROR}）→ {@link FailoverDecision#L1}
 *       （换渠道：同一 Provider 下换 Key/Endpoint）</li>
 *   <li>{@link ProviderErrorType#UNKNOWN_ERROR} → {@link FailoverDecision#L2}
 *       （模型能力问题，换模型降级）</li>
 * </ul>
 *
 * <p>null 输入处理：返回 {@link FailoverDecision#NONE}。null 表示调用方编程错误或未分类错误，
 * 此时无法判定故障归因，直接抛出原异常避免掩盖问题。</p>
 */
@Component
public class ErrorClassifier {

    /** 分流表：ProviderErrorType → FailoverDecision 的静态映射 */
    private static final Map<ProviderErrorType, FailoverDecision> DECISION_TABLE = new EnumMap<>(ProviderErrorType.class);

    static {
        // 请求级错误：换哪都无效，直接抛出原异常
        DECISION_TABLE.put(ProviderErrorType.INVALID_REQUEST, FailoverDecision.NONE);

        // 共因故障：换渠道（同一 Provider 下换 Key/Endpoint）
        DECISION_TABLE.put(ProviderErrorType.AUTHENTICATION_ERROR, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.RATE_LIMIT_ERROR, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.QUOTA_EXCEEDED, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.TIMEOUT_ERROR, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.UPSTREAM_ERROR, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.SERVICE_UNAVAILABLE, FailoverDecision.L1);
        DECISION_TABLE.put(ProviderErrorType.NETWORK_ERROR, FailoverDecision.L1);

        // 模型能力问题：换模型降级
        DECISION_TABLE.put(ProviderErrorType.UNKNOWN_ERROR, FailoverDecision.L2);
    }

    /**
     * 按供应商错误类型分类故障转移决策
     *
     * @param type 供应商错误类型；为 null 时返回 {@link FailoverDecision#NONE}
     * @return 故障转移决策（L1/L2/NONE）
     */
    public FailoverDecision classify(ProviderErrorType type) {
        if (type == null) {
            // null 表示编程错误或未分类错误，无法判定故障归因，直接抛出不转移
            return FailoverDecision.NONE;
        }
        // 兜底返回 L2：防御性处理未来新增枚举值未及时更新分流表的情况，按模型能力问题降级
        return DECISION_TABLE.getOrDefault(type, FailoverDecision.L2);
    }
}
