package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 渠道模型关联数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "channel_models", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cm_channel_model", columnNames = {"channel_id", "model_id"})
})
public class ChannelModelDo extends BaseDo {

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "upstream_model_name", length = 256)
    private String upstreamModelName;

    @Column(name = "input_price", precision = 18, scale = 6)
    private BigDecimal inputPrice;

    @Column(name = "output_price", precision = 18, scale = 6)
    private BigDecimal outputPrice;

    @Column(name = "reasoning_price", precision = 18, scale = 6)
    private BigDecimal reasoningPrice;

    @Column(name = "cache_read_price", precision = 18, scale = 6)
    private BigDecimal cacheReadPrice;

    @Column(name = "cache_write_price", precision = 18, scale = 6)
    private BigDecimal cacheWritePrice;

    @Column(name = "input_audio_price", precision = 18, scale = 6)
    private BigDecimal inputAudioPrice;

    @Column(name = "output_audio_price", precision = 18, scale = 6)
    private BigDecimal outputAudioPrice;

    @Column(name = "quota_limit")
    private Long quotaLimit;

    @Column(name = "state", nullable = false, length = 32)
    private String state;
}