package com.codingas.gateway.domain.supply.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 渠道凭证实体（替代 ProductApiKey）
 *
 * <p>供应商侧认证密钥，用于调用供应商 API。</p>
 * <p>一个渠道可配置多个凭证，支持密钥轮换、负载均衡和故障转移。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "apiKeyPlain")
@DomainEntity
@Slf4j
public class ChannelCredential extends BaseEntity {

    /** 关联的渠道 ID */
    private Long channelId;

    /** 密钥名称 */
    private String name;

    /** Key 明文（创建时设置，查询时由基础设施层解密填充） */
    private String apiKeyPlain;

    /** Key 加密存储 */
    private String apiKeyEncrypted;

    /** Key 前缀，用于识别 */
    private String apiKeyPrefix;

    /** 密钥别名 */
    private String keyAlias;

    /** 负载均衡权重 */
    private Integer weight;

    /** 故障转移优先级（数值越小优先级越高） */
    private Integer priority;

    /** 凭证状态 */
    private CredentialState state = CredentialState.ACTIVE;

    /** 最后使用时间 */
    private Instant lastUsedAt;

    /**
     * 检查凭证是否可用
     */
    public boolean isAvailable() {
        return CredentialState.ACTIVE.equals(state);
    }
}