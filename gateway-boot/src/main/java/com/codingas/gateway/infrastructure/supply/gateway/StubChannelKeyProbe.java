package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.application.supply.dto.KeyTestResult;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.gateway.ChannelKeyProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ChannelKeyProbe 的占位实现
 *
 * <p>占位实现，第 9 章（前端测试入口归一）会接入真实出站；
 * 当前一律返回 PASS + 空模型列表，便于联调与契约测试。</p>
 *
 * <p>仅在缺失 Bean 时兜底，使用 {@code @ConditionalOnMissingBean} 让真实实现可覆盖。</p>
 */
@Component
@Slf4j
public class StubChannelKeyProbe implements ChannelKeyProbe {

    /**
     * 占位实现：对所有 Key 返回 PASS（无可用模型列表）
     *
     * <p>注意：聚合规则要求 PASS 且有可用模型才计入 HEALTHY；占位返回空模型列表，
     * 因此整个渠道在未接入真实出站前仍被聚合为 FAILED。这是预期行为，
     * 提示前端"未接入真实出站时不要展示成误判 HEALTHY"。</p>
     */
    @Override
    public KeyTestResult test(Channel channel, ChannelCredential credential) {
        log.debug("StubChannelKeyProbe.test: channelId={}, credentialId={}",
                channel.getId(), credential.getId());
        return KeyTestResult.pass(
                credential.getId(),
                credential.getApiKeyPlain() != null ? credential.getApiKeyPlain()
                        : credential.getApiKeyPrefix(),
                List.of(),
                0L
        );
    }
}
