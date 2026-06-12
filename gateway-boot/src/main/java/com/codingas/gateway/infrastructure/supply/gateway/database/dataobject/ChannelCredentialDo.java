package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;

/**
 * 渠道凭证数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(exclude = "apiKeyPlain")
@Entity
@Table(name = "channel_credentials")
public class ChannelCredentialDo extends BaseDo {

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "api_key_plain", length = 512)
    private String apiKeyPlain;

    @Column(name = "encrypted_api_key", length = 1024)
    private String apiKeyEncrypted;

    @Column(name = "api_key_prefix", length = 32)
    private String apiKeyPrefix;

    @Column(name = "key_alias", length = 128)
    private String keyAlias;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}