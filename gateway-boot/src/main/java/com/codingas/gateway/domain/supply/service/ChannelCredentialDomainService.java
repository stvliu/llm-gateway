package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.CredentialState;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelCredentialGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道凭证领域服务
 *
 * <p>封装凭证相关的核心业务逻辑，替代原 ProductApiKeyDomainService。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelCredentialDomainService {

    private final ChannelCredentialGateway credentialGateway;

    /**
     * 创建凭证
     */
    public ChannelCredential create(ChannelCredential credential) {
        return credentialGateway.save(credential);
    }

    /**
     * 启用凭证
     */
    public ChannelCredential enable(Long id) {
        ChannelCredential credential = credentialGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CREDENTIAL_NOT_FOUND", "凭证不存在: " + id));
        credential.setState(CredentialState.ACTIVE);
        return credentialGateway.save(credential);
    }

    /**
     * 禁用凭证
     */
    public ChannelCredential disable(Long id) {
        ChannelCredential credential = credentialGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CREDENTIAL_NOT_FOUND", "凭证不存在: " + id));
        credential.setState(CredentialState.INACTIVE);
        return credentialGateway.save(credential);
    }

    /**
     * 软删除凭证
     */
    public void delete(Long id) {
        ChannelCredential credential = credentialGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CREDENTIAL_NOT_FOUND", "凭证不存在: " + id));
        credentialGateway.deleteById(id);
    }

    /**
     * 查找渠道的活跃凭证
     */
    public List<ChannelCredential> findActiveByChannelId(Long channelId) {
        return credentialGateway.findActiveByChannelId(channelId);
    }

    /**
     * 更新最后使用时间
     */
    public void updateLastUsedAt(Long id) {
        credentialGateway.updateLastUsedAt(id);
    }
}