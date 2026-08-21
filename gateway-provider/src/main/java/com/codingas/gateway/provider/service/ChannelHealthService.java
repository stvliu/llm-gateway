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
package com.codingas.gateway.provider.service;

import com.codingas.gateway.provider.upstream.AuthStatus;
import com.codingas.gateway.provider.dto.ChannelHealthResult;
import com.codingas.gateway.provider.dto.KeyMatrixRow;
import com.codingas.gateway.provider.upstream.KeyTestResult;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelHealthSource;
import com.codingas.gateway.provider.channel.ChannelHealthStatus;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.channel.ChannelKeyProbe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 渠道健康检查应用服务
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>并发对渠道下所有 Key 发起连通性探测，单 Key 5s 超时，总体 30s 超时</li>
 *   <li>按"全部 PASS+模型 → HEALTHY、部分通过 → DEGRADED、全部失败 → FAILED、无 Key → UNKNOWN"聚合</li>
 *   <li>仅 CARD/DRAWER 来源持久化健康字段（last-write-wins，不加乐观锁）</li>
 *   <li>持久化失败兜底：异常仅记日志，不冒出主流程</li>
 * </ul>
 *
 * <p>通过独立 {@code healthCheckExecutor} 隔离线程池，避免阻塞 Tomcat 主线程。</p>
 */
@Service
@Slf4j
public class ChannelHealthService {

    /** 单 Key 测试超时（秒） */
    private static final long PER_KEY_TIMEOUT_SECONDS = 5L;

    /** 总体测试超时（秒） */
    private static final long TOTAL_TIMEOUT_SECONDS = 30L;

    private final ChannelGateway channelGateway;
    private final ChannelCredentialGateway credentialGateway;
    private final ChannelKeyProbe channelKeyProbe;
    private final Executor healthCheckExecutor;

    /**
     * 构造器注入；{@code healthCheckExecutor} 来自 {@code HealthCheckExecutorConfig}。
     */
    public ChannelHealthService(ChannelGateway channelGateway,
                                ChannelCredentialGateway credentialGateway,
                                ChannelKeyProbe channelKeyProbe,
                                @Qualifier("healthCheckExecutor") Executor healthCheckExecutor) {
        this.channelGateway = channelGateway;
        this.credentialGateway = credentialGateway;
        this.channelKeyProbe = channelKeyProbe;
        this.healthCheckExecutor = healthCheckExecutor;
    }

    /**
     * 触发渠道连通性测试，按聚合规则写入健康状态。
     *
     * @param channelId 渠道 ID
     * @param source    触发来源；PRECHECK 时跳过持久化
     * @return 测试矩阵 + 聚合状态
     * @throws GatewayRequestException 渠道不存在
     */
    @Transactional
    public ChannelHealthResult check(Long channelId, ChannelHealthSource source) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new GatewayRequestException(
                        "CHANNEL_NOT_FOUND", "渠道不存在: " + channelId));

        List<ChannelCredential> credentials = Optional.ofNullable(
                credentialGateway.findByChannelId(channelId)).orElse(Collections.emptyList());

        Instant startedAt = Instant.now();
        List<KeyTestResult> results = runProbes(channel, credentials);
        Instant finishedAt = Instant.now();

        ChannelHealthStatus aggregate = aggregate(results);

        // 仅 CARD / DRAWER 持久化；PRECHECK 完全短路
        if (source != ChannelHealthSource.PRECHECK) {
            persistHealth(channel, aggregate, source);
        } else {
            log.debug("PRECHECK 来源不持久化健康字段: channelId={}", channelId);
        }

        List<KeyMatrixRow> matrix = results.stream().map(this::toMatrixRow).toList();
        return new ChannelHealthResult(channelId, aggregate, startedAt, finishedAt, matrix);
    }

    /**
     * 并发执行单 Key 探测；单 Key 5s 超时，整体 30s 超时。
     *
     * <p>所有失败/超时通过 {@code exceptionally} 接住，不会抛到调用方；
     * 调用方只需读取已完成 Future 的值。</p>
     */
    private List<KeyTestResult> runProbes(Channel channel, List<ChannelCredential> credentials) {
        if (credentials.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<KeyTestResult>> futures = new ArrayList<>(credentials.size());
        for (ChannelCredential credential : credentials) {
            CompletableFuture<KeyTestResult> future = CompletableFuture
                    .supplyAsync(() -> channelKeyProbe.test(channel, credential), healthCheckExecutor)
                    .orTimeout(PER_KEY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.warn("Key 探测异常或超时: channelId={}, credentialId={}, err={}",
                                channel.getId(), credential.getId(), ex.toString());
                        String keyHint = credential.getApiKeyPlain() != null
                                ? credential.getApiKeyPlain()
                                : credential.getApiKeyPrefix();
                        return KeyTestResult.timeout(credential.getId(), keyHint);
                    });
            futures.add(future);
        }

        // 总超时 30s；超时的单 Key 已被 exceptionally 接住，这里仅吞掉外层超时
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
        } catch (Exception ignored) {
            log.warn("健康测试总超时，将以已完成结果聚合: channelId={}", channel.getId());
        }

        List<KeyTestResult> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            CompletableFuture<KeyTestResult> f = futures.get(i);
            if (f.isDone() && !f.isCompletedExceptionally()) {
                results.add(f.join());
            } else {
                ChannelCredential credential = credentials.get(i);
                String keyHint = credential.getApiKeyPlain() != null
                        ? credential.getApiKeyPlain()
                        : credential.getApiKeyPrefix();
                results.add(KeyTestResult.timeout(credential.getId(), keyHint));
            }
        }
        return results;
    }

    /**
     * 健康聚合规则（提取为静态方法便于单测）
     *
     * <ul>
     *   <li>结果列表为空 → UNKNOWN</li>
     *   <li>全部 PASS 且各自至少 1 个可用模型 → HEALTHY</li>
     *   <li>全部失败（无 PASS+模型）→ FAILED</li>
     *   <li>其余 → DEGRADED</li>
     * </ul>
     */
    public static ChannelHealthStatus aggregate(List<KeyTestResult> results) {
        if (results == null || results.isEmpty()) {
            return ChannelHealthStatus.UNKNOWN;
        }
        long pass = results.stream()
                .filter(r -> r.auth() == AuthStatus.PASS && !r.availableModels().isEmpty())
                .count();
        long total = results.size();
        if (pass == total) {
            return ChannelHealthStatus.HEALTHY;
        }
        if (pass == 0) {
            return ChannelHealthStatus.FAILED;
        }
        return ChannelHealthStatus.DEGRADED;
    }

    /**
     * 持久化健康字段；任何异常仅记日志，不冒出主流程
     */
    private void persistHealth(Channel channel, ChannelHealthStatus status, ChannelHealthSource source) {
        try {
            channel.setLastHealthCheckAt(Instant.now());
            channel.setLastHealthStatus(status);
            channel.setLastHealthSource(source);
            channelGateway.save(channel);
            log.info("健康状态写入: channelId={}, status={}, source={}",
                    channel.getId(), status, source);
        } catch (Exception e) {
            log.error("健康状态写入失败（已兜底吞掉）: channelId={}", channel.getId(), e);
        }
    }

    /**
     * 将 {@link KeyTestResult} 转换为脱敏后的 {@link KeyMatrixRow}
     */
    private KeyMatrixRow toMatrixRow(KeyTestResult r) {
        return new KeyMatrixRow(
                r.credentialId(),
                maskKey(r.apiKeyPlain()),
                r.auth(),
                r.errorMessage(),
                r.availableModels(),
                r.latencyMs()
        );
    }

    /**
     * Key 脱敏：保留前缀（最多 4 位）+ ***...+ 后 4 位
     */
    static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "***";
        }
        String trimmed = key.trim();
        if (trimmed.length() <= 8) {
            return "***";
        }
        String prefix = trimmed.substring(0, Math.min(4, trimmed.length()));
        String suffix = trimmed.substring(trimmed.length() - 4);
        return String.format(Locale.ROOT, "%s***...%s", prefix, suffix);
    }
}
