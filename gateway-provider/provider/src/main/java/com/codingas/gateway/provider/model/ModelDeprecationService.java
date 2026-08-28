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
package com.codingas.gateway.provider.model;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 模型废弃自动化服务
 *
 * <p>承载两条自动信号通道的状态落地：运行期确认（{@link #markDeprecated}）与
 * 列表探测（{@link #markInstanceDeprecated}/{@link #restoreInstance}）。
 * 所有状态变更写管理操作审计（AuditLog）。幂等：已废弃/已处于目标状态的跳过。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelDeprecationService {

    private final ModelRepository modelRepository;
    private final ModelInstanceRepository instanceRepository;
    private final AuditLogRepository auditLogRepository;

    /**
     * 运行期确认模型已废弃：幂等设置 deprecatedAt + deprecationMessage，
     * 该模型所有实例转 RETIRED（系统强信号，直接设置），写审计。
     *
     * @param modelName 模型名（用户面标识）
     * @param reason    废弃原因
     */
    @Transactional
    public void markDeprecated(String modelName, String reason) {
        Model model = modelRepository.findByModelName(modelName).orElse(null);
        if (model == null) {
            log.warn("废弃确认目标模型不存在, modelName={}", modelName);
            return;
        }
        if (model.getDeprecatedAt() != null) {
            log.debug("模型已废弃，跳过重复确认, modelName={}", modelName);
            return;
        }
        model.setDeprecatedAt(Instant.now());
        model.setDeprecationMessage(reason);
        modelRepository.save(model);
        // 该模型所有实例转 RETIRED（上游已确认不可用，系统级强制）
        List<ModelInstance> instances = instanceRepository.findByModelId(model.getId());
        for (ModelInstance instance : instances) {
            if (instance.getState() != ModelInstance.State.RETIRED) {
                instance.setState(ModelInstance.State.RETIRED);
                instanceRepository.save(instance);
            }
        }
        writeAudit("MODEL_DEPRECATED", "Model:" + modelName, "SUCCESS");
        log.info("模型已自动标记废弃, modelName={}, reason={}", modelName, reason);
    }

    /**
     * 探测确认渠道不再提供该模型：仅 ACTIVE→DEPRECATED（即将废弃，继续路由）。
     * 非 ACTIVE 实例（RETIRED/PENDING/SUSPENDED/DEPRECATED）直接跳过，
     * 避免把终态（RETIRED）实例"复活"或对未就绪实例做非法转移。
     *
     * @param instanceId 模型实例 ID
     */
    @Transactional
    public void markInstanceDeprecated(Long instanceId) {
        ModelInstance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || instance.getState() != ModelInstance.State.ACTIVE) {
            return;
        }
        instance.setState(ModelInstance.State.DEPRECATED);
        instanceRepository.save(instance);
        writeAudit("MODEL_INSTANCE_DEPRECATED", "ModelInstance:" + instanceId, "SUCCESS");
        log.info("模型实例标记即将废弃, id={}", instanceId);
    }

    /**
     * 探测发现模型重新出现：DEPRECATED→ACTIVE（自动恢复）
     *
     * @param instanceId 模型实例 ID
     */
    @Transactional
    public void restoreInstance(Long instanceId) {
        ModelInstance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null || instance.getState() != ModelInstance.State.DEPRECATED) {
            return;
        }
        instance.setState(ModelInstance.State.ACTIVE);
        instanceRepository.save(instance);
        writeAudit("MODEL_INSTANCE_RESTORED", "ModelInstance:" + instanceId, "SUCCESS");
        log.info("模型实例已自动恢复, id={}", instanceId);
    }

    /** 写管理操作审计（自动系统操作，userId 置 0 表示系统主体） */
    private void writeAudit(String action, String resource, String result) {
        AuditLog auditLog = new AuditLog();
        // user_id 列为 NOT NULL，系统自动操作以 0 表示（同 AuditEventListener 惯例）
        auditLog.setUserId(0L);
        auditLog.setAction(action);
        auditLog.setResource(resource);
        auditLog.setResult(result);
        try {
            auditLogRepository.saveAuditLog(auditLog);
        } catch (Exception e) {
            log.warn("写入废弃审计失败: {}", e.getMessage());
        }
    }
}
