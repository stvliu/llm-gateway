package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "channel_endpoints")
public class ChannelEndpointDo extends BaseDo {

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 32)
    private com.codingas.gateway.domain.supply.enums.Protocol protocol;

    @Column(name = "endpoint_url", nullable = false, length = 512)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private com.codingas.gateway.domain.supply.enums.ChannelEndpointState state;
}
