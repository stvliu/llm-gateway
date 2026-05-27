package com.codingas.gateway.infrastructure.iam.gateway.database.dataobject;

import jakarta.persistence.*;

/**
 * UserApiKey 与 Channel 的多对多关联数据对象
 */
@Entity
@Table(name = "user_api_key_channels")
public class UserApiKeyChannelDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_api_key_id", nullable = false)
    private Long userApiKeyId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserApiKeyId() { return userApiKeyId; }
    public void setUserApiKeyId(Long userApiKeyId) { this.userApiKeyId = userApiKeyId; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
}
