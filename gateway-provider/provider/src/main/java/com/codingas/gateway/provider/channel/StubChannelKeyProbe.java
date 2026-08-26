/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.provider.channel;

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
