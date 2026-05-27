package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道模型关联创建请求
 */
@Data
public class ChannelModelCreateRequest {

    @NotNull(message = "模型 ID 不能为空")
    private Long modelSpecId;
}