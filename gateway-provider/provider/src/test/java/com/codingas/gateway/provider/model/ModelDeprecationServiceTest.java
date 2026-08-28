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
