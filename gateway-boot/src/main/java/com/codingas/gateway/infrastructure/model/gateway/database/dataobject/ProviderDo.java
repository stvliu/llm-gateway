package com.codingas.gateway.infrastructure.model.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.model.enums.ProviderState;
import jakarta.persistence.*;
import lombok.*;

/**
 * 提供商 DO
 *
 * <p>JPA 实体，对应数据库 providers 表。</p>
 */
@Entity
@Table(name = "providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDo extends BaseDo {

    @Column(name = "provider_name", nullable = false, length = 128)
    private String name;

    @Column(name = "provider_type", nullable = false)
    private String type;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "api_doc_url", length = 512)
    private String apiDocUrl;

    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 调用超时时间（毫秒）
     */
    @Column(name = "timeout")
    private Integer timeout = 30000;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ProviderState state = ProviderState.ACTIVE;
}