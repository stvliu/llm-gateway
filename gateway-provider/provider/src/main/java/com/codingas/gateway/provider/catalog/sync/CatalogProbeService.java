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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialService;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.channel.ChannelService;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelDeprecationService;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上游列表探测编排服务（提前预警通道）
 *
 * <p>对每个参与探测的渠道（有端点、有凭证、协议支持列表 API）拉取上游模型 ID 集合，
 * 对比该渠道 ModelInstance.upstreamModelName：消失连续 N 次（内存计数）转 DEPRECATED
 * （即将废弃，继续路由）；重新出现自动恢复 ACTIVE。结果写入 catalog_sync_logs
 * （result=PROBE）。总开关/探测开关关闭时不执行。</p>
 *
 * <p>仅探测显式映射了 upstreamModelName 或能解析到 Model 名的实例：
 * upstreamModelName 为 null（默认等于 Model.modelName）时经
 * {@link ModelRepository#findById} 兜底解析；仍无法解析（Model 不存在）则跳过该实例，
 * 避免误判。单渠道探测失败（{@link CatalogSyncException}）不阻断其他渠道。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogProbeService {

    /** 连续消失计数：instanceId → 计数（内存态，重启清零） */
    private final ConcurrentHashMap<Long, Integer> missingCounts = new ConcurrentHashMap<>();

    private final ChannelService channelService;
    private final ChannelCredentialService credentialService;
    private final ModelInstanceRepository instanceRepository;
    private final ModelRepository modelRepository;
    private final UpstreamModelProbeClient probeClient;
    private final ModelDeprecationService deprecationService;
    private final CatalogSyncLogRepository logRepository;
    private final SystemSettingService settingService;

    /**
     * 执行一轮上游列表探测
     *
     * <p>总开关（catalog.deprecation.enabled）或探测子开关
     * （catalog.deprecation.probe.enabled）关闭时直接返回空报告不执行。
     * 遍历渠道逐渠道探测；单渠道失败计入 failedCount 并继续其他渠道。
     * 结果写入探测日志（result=PROBE；整体异常或存在渠道失败时 FAILURE）。</p>
     *
     * @return 探测报告
     */
    public CatalogSyncReport probe() {
        Instant startedAt = Instant.now();
        CatalogSyncReport report = CatalogSyncReport.builder()
                .success(true)
                .syncedAt(startedAt)
                .messages(new ArrayList<>())
                .build();
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.probe.enabled", true)) {
            log.debug("上游列表探测已关闭，跳过");
            return report;
        }
        int confirmCount = settingService.getInt("catalog.deprecation.confirm-count", 3);
        try {
            List<Channel> channels = channelService.getAll();
            for (Channel channel : channels) {
                probeChannel(channel, confirmCount, report);
            }
            saveProbeLog(report, null, startedAt);
        } catch (RuntimeException e) {
            saveProbeLog(report, e.getMessage(), startedAt);
            log.error("上游列表探测失败: {}", e.getMessage(), e);
            throw e;
        }
        return report;
    }

    /**
     * 探测单个渠道：拉上游列表 → 对比实例 → 消失计数/恢复
     *
     * <p>协议不支持列表 API（非 OPENAI/ANTHROPIC/GEMINI）或无凭证的渠道直接跳过；
     * 上游拉取失败（凭证失效/网络异常）仅记录消息，不触发废弃，也不阻断其他渠道。</p>
     *
     * @param channel      渠道
     * @param confirmCount 消失确认阈值（连续 N 次缺失才转 DEPRECATED）
     * @param report       探测报告（累加计数与消息）
     */
    private void probeChannel(Channel channel, int confirmCount, CatalogSyncReport report) {
        List<ChannelEndpoint> endpoints = channelService.getEndpoints(channel.getId());
        List<ChannelEndpoint> probeable = endpoints.stream()
                .filter(e -> e.getProtocol() == Protocol.OPENAI
                        || e.getProtocol() == Protocol.ANTHROPIC
                        || e.getProtocol() == Protocol.GEMINI)
                .toList();
        if (probeable.isEmpty()) {
            return;
        }
        List<ChannelCredential> credentials = credentialService.listByChannelId(channel.getId());
        if (credentials.isEmpty()) {
            return;
        }
        ChannelEndpoint endpoint = probeable.get(0);
        Set<String> upstreamIds;
        try {
            upstreamIds = probeClient.fetchModelIds(endpoint, credentials.get(0).getApiKeyPlain());
        } catch (CatalogSyncException e) {
            // 单渠道探测失败不阻断其他渠道（凭证失效/网络异常不触发废弃），
            // 但计入失败数并记入消息，最终探测日志以 FAILURE 呈现该渠道失败
            report.incrementFailed();
            report.addMessage("渠道探测失败: " + channel.getName() + " - " + e.getMessage());
            return;
        }
        for (ModelInstance instance : instanceRepository.findByChannelId(channel.getId())) {
            String modelName = resolveUpstreamName(instance);
            if (modelName == null) {
                // upstreamModelName 为空且 Model 无法解析：跳过该实例，不误判
                continue;
            }
            if (upstreamIds.contains(modelName)) {
                // 重新出现：恢复（若曾标记）
                missingCounts.remove(instance.getId());
                if (instance.getState() == ModelInstance.State.DEPRECATED) {
                    deprecationService.restoreInstance(instance.getId());
                    report.incrementUpdated();
                }
            } else {
                // 消失：连续计数，达到阈值转 DEPRECATED（仅 ACTIVE 可转移）
                int count = missingCounts.merge(instance.getId(), 1, Integer::sum);
                if (count >= confirmCount) {
                    missingCounts.remove(instance.getId());
                    if (instance.getState() == ModelInstance.State.ACTIVE) {
                        deprecationService.markInstanceDeprecated(instance.getId());
                        report.incrementUpdated();
                    }
                }
            }
        }
    }

    /**
     * 解析实例的上游模型名
     *
     * <p>优先取显式映射的 upstreamModelName；为空时兜底取 Model.modelName
     * （在循环内查，量小可接受）；Model 不存在则返回 null，由调用方跳过该实例。</p>
     *
     * @param instance 模型实例
     * @return 上游模型名；无法解析时返回 null
     */
    private String resolveUpstreamName(ModelInstance instance) {
        if (instance.getUpstreamModelName() != null) {
            return instance.getUpstreamModelName();
        }
        Model model = modelRepository.findById(instance.getModelId()).orElse(null);
        return model != null ? model.getModelName() : null;
    }

    /**
     * 探测日志落 catalog_sync_logs（result=PROBE/FAILURE）
     *
     * <p>整体异常或存在渠道探测失败（failedCount &gt; 0）时 result 记 FAILURE，
     * 否则记 PROBE；日志保存失败仅告警，不中断探测主流程。</p>
     *
     * @param report   探测报告
     * @param error    整体失败原因（成功时为 null）
     * @param syncedAt 探测开始时间
     */
    private void saveProbeLog(CatalogSyncReport report, String error, Instant syncedAt) {
        boolean hasChannelFailure = report.getFailedCount() > 0;
        boolean failed = error != null || hasChannelFailure;
        CatalogSyncLog syncLog = new CatalogSyncLog();
        // 触发来源标记 PROBE：与目录同步（SYNC）区分，供探测周期按来源独立判断
        syncLog.setTriggeredBy("PROBE");
        syncLog.setResult(failed ? "FAILURE" : "PROBE");
        syncLog.setAddedCount(0);
        syncLog.setUpdatedCount(report.getUpdatedCount());
        syncLog.setSkippedCount(report.getSkippedCount());
        syncLog.setFailedCount(report.getFailedCount());
        syncLog.setMessage(error != null ? error
                : hasChannelFailure ? "上游列表探测完成，失败渠道数=" + report.getFailedCount()
                : "上游列表探测完成");
        syncLog.setSyncedAt(syncedAt);
        try {
            logRepository.save(syncLog);
        } catch (Exception e) {
            log.warn("保存探测日志失败: {}", e.getMessage());
        }
    }
}
