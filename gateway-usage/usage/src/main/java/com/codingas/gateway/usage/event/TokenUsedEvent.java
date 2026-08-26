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
package com.codingas.gateway.usage.event;

import com.codingas.gateway.common.event.BizEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Token 使用事件
 *
 * <p>当 LLM 请求完成时发布，包含 Token 使用量信息。</p>
 *
 * @param userId 用户 ID
 * @param apiKeyId API Key ID
 * @param teamId 团队 ID
 * @param model 模型代码
 * @param provider 提供商编码
 * @param promptTokens 输入 Token 数
 * @param completionTokens 输出 Token 数
 * @param cost 费用（基于提供商定价计算）
 * @param traceId OpenTelemetry Trace ID
 * @param occurredOn 发生时间
 */
public record TokenUsedEvent(
        Long userId,
        Long apiKeyId,
        Long teamId,
        String model,
        String provider,
        int promptTokens,
        int completionTokens,
        BigDecimal cost,
        String traceId,
        Instant occurredOn
) implements BizEvent {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .userId(userId)
                .apiKeyId(apiKeyId)
                .teamId(teamId)
                .model(model)
                .provider(provider)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .cost(cost)
                .traceId(traceId)
                .occurredOn(occurredOn);
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    /**
     * 计算总 Token 数
     */
    public int totalTokens() {
        return promptTokens + completionTokens;
    }

    public static class Builder {
        private Long userId;
        private Long apiKeyId;
        private Long teamId;
        private String model;
        private String provider;
        private int promptTokens;
        private int completionTokens;
        private BigDecimal cost = BigDecimal.ZERO;
        private String traceId;
        private Instant occurredOn = Instant.now();

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder apiKeyId(Long apiKeyId) {
            this.apiKeyId = apiKeyId;
            return this;
        }

        public Builder teamId(Long teamId) {
            this.teamId = teamId;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder promptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder cost(BigDecimal cost) {
            this.cost = cost;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder occurredOn(Instant occurredOn) {
            this.occurredOn = occurredOn;
            return this;
        }

        public TokenUsedEvent build() {
            return new TokenUsedEvent(
                    userId, apiKeyId, teamId, model, provider,
                    promptTokens, completionTokens, cost, traceId, occurredOn
            );
        }
    }
}
