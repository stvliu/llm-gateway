package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.enums.ProviderType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 提供商实体
 *
 * <p>表示 AI 模型服务提供商。</p>
 */
@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Provider extends BaseEntity {

    @Column(name = "provider_code", nullable = false, unique = true, length = 64)
    private String providerCode;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderTypeEnum providerType;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "api_key_encrypted")
    private String apiKeyEncrypted;

    @Column(name = "enabled")
    private Boolean enabled;

    /**
     * 转换为通用 ProviderType 枚举
     *
     * @return common.enums.ProviderType
     */
    public ProviderType toProviderType() {
        if (providerType == null) {
            return null;
        }
        switch (providerType) {
            case OPENAI:
                return ProviderType.OPENAI;
            case ANTHROPIC:
                return ProviderType.ANTHROPIC;
            case ZHIPU:
                return ProviderType.ZHIPU;
            case DOUBAO:
            case CUSTOM:
            default:
                return ProviderType.OTHER;
        }
    }

    public enum ProviderTypeEnum {
        OPENAI, ANTHROPIC, ZHIPU, DOUBAO, CUSTOM
    }
}
