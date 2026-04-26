package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    private ProviderType providerType;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "api_key_encrypted")
    private String apiKeyEncrypted;

    @Column(name = "enabled")
    private Boolean enabled;

    public enum ProviderType {
        OPENAI, ANTHROPIC, ZHIPU, DOUBAO, CUSTOM
    }
}
