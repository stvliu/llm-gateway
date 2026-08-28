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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelDeprecationService 单元测试（模型废弃自动化）
 *
 * <p>覆盖运行期确认（markDeprecated）与探测确认（markInstanceDeprecated/restoreInstance）：
 * 状态字段落地、实例状态流转、幂等跳过与审计写入。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelDeprecationService 模型废弃自动化")
class ModelDeprecationServiceTest {

    @Mock private ModelRepository modelRepository;
    @Mock private ModelInstanceRepository instanceRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private ModelDeprecationService service;

    private Model model() {
        Model m = new Model();
        m.setId(1L);
        m.setModelName("gpt-4");
        return m;
    }

    @Test
    @DisplayName("markDeprecated 幂等设置 deprecatedAt 并转 RETIRED + 审计")
    void markDeprecated_setsFieldsAndRetiresInstances() {
        Model m = model();
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.ACTIVE);
        when(instanceRepository.findByModelId(1L)).thenReturn(List.of(inst));

        service.markDeprecated("gpt-4", "上游确认模型已废弃（model_not_found）");

        assertThat(m.getDeprecatedAt()).isNotNull();
        assertThat(m.getDeprecationMessage()).contains("model_not_found");
        assertThat(inst.getState()).isEqualTo(ModelInstance.State.RETIRED);
        verify(modelRepository).save(m);
        verify(auditLogRepository).saveAuditLog(any(AuditLog.class));
    }

    @Test
    @DisplayName("markDeprecated 已废弃时幂等跳过（不重复审计）")
    void markDeprecated_alreadyDeprecated_skips() {
        Model m = model();
        m.setDeprecatedAt(Instant.now());
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));

        service.markDeprecated("gpt-4", "again");

        verify(modelRepository, never()).save(any(Model.class));
        verify(auditLogRepository, never()).saveAuditLog(any(AuditLog.class));
    }

    @Test
    @DisplayName("markDeprecated 写审计时 userId 置 0（系统主体，规避 NOT NULL）")
    void markDeprecated_auditHasSystemUserId() {
        Model m = model();
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));
        when(instanceRepository.findByModelId(1L)).thenReturn(List.of());

        service.markDeprecated("gpt-4", "reason");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAuditLog(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(0L);
        assertThat(captor.getValue().getAction()).isEqualTo("MODEL_DEPRECATED");
    }

    @Test
    @DisplayName("markDeprecated 实例已 RETIRED 时跳过（不重复 save）")
    void markDeprecated_retiredInstance_notSavedAgain() {
        Model m = model();
        when(modelRepository.findByModelName("gpt-4")).thenReturn(Optional.of(m));
        ModelInstance active = new ModelInstance();
        active.setId(10L);
        active.setState(ModelInstance.State.ACTIVE);
        ModelInstance retired = new ModelInstance();
        retired.setId(11L);
        retired.setState(ModelInstance.State.RETIRED);
        when(instanceRepository.findByModelId(1L)).thenReturn(List.of(active, retired));

        service.markDeprecated("gpt-4", "reason");

        assertThat(active.getState()).isEqualTo(ModelInstance.State.RETIRED);
        assertThat(retired.getState()).isEqualTo(ModelInstance.State.RETIRED);
        verify(instanceRepository).save(active);
        verify(instanceRepository, never()).save(retired);
    }

    @Test
    @DisplayName("markInstanceDeprecated ACTIVE→DEPRECATED")
    void markInstanceDeprecated_transitions() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.ACTIVE);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.markInstanceDeprecated(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.DEPRECATED);
        verify(instanceRepository).save(inst);
    }

    @Test
    @DisplayName("markInstanceDeprecated 对 RETIRED 实例跳过（不复活终态）")
    void markInstanceDeprecated_retired_skips() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.RETIRED);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.markInstanceDeprecated(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.RETIRED);
        verify(instanceRepository, never()).save(any(ModelInstance.class));
        verify(auditLogRepository, never()).saveAuditLog(any(AuditLog.class));
    }

    @Test
    @DisplayName("markInstanceDeprecated 对 PENDING 实例跳过（未就绪不转移）")
    void markInstanceDeprecated_pending_skips() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.PENDING);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.markInstanceDeprecated(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.PENDING);
        verify(instanceRepository, never()).save(any(ModelInstance.class));
    }

    @Test
    @DisplayName("restoreInstance DEPRECATED→ACTIVE")
    void restoreInstance_transitions() {
        ModelInstance inst = new ModelInstance();
        inst.setId(10L);
        inst.setState(ModelInstance.State.DEPRECATED);
        when(instanceRepository.findById(10L)).thenReturn(Optional.of(inst));

        service.restoreInstance(10L);

        assertThat(inst.getState()).isEqualTo(ModelInstance.State.ACTIVE);
        verify(instanceRepository).save(inst);
    }
}
