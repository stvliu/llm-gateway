package com.codingas.gateway.application.channelcredential;

import com.codingas.gateway.application.channel.dto.ApiKeyTestResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateRequest;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialCreateResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialDetailResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialResponse;
import com.codingas.gateway.application.channelcredential.dto.ChannelCredentialUpdateRequest;
import com.codingas.gateway.domain.supply.enums.CredentialState;

import java.util.List;

/**
 * 渠道凭证应用服务接口
 */
public interface ChannelCredentialService {

    /**
     * 创建渠道凭证
     */
    ChannelCredentialCreateResponse create(Long channelId, ChannelCredentialCreateRequest request);

    /**
     * 根据 ID 获取渠道凭证（校验渠道归属，不含明文）
     */
    ChannelCredentialResponse getById(Long channelId, Long id);

    /**
     * 根据 ID 获取渠道凭证详情（含明文，用于页面复制）
     */
    ChannelCredentialDetailResponse getDetailById(Long channelId, Long id);

    /**
     * 获取渠道下的所有凭证
     */
    List<ChannelCredentialResponse> listByChannelId(Long channelId);

    /**
     * 更新渠道凭证（校验渠道归属）
     */
    ChannelCredentialResponse update(Long channelId, Long id, ChannelCredentialUpdateRequest request);

    /**
     * 删除渠道凭证（校验渠道归属）
     */
    void delete(Long channelId, Long id);

    /**
     * 测试 API Key 是否有效
     */
    ApiKeyTestResponse testApiKey(Long channelId, Long id);
}
