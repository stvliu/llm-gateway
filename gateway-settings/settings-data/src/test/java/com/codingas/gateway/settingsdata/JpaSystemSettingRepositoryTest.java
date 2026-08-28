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
package com.codingas.gateway.settingsdata;

import com.codingas.gateway.settings.SystemSetting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * JpaSystemSettingRepository 单元测试：mock JPA Repository 验证委托与 实体↔DO 双向转换
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaSystemSettingRepository 单元测试")
class JpaSystemSettingRepositoryTest {

    @Mock
    private SystemSettingJpaRepository jpaRepository;

    private JpaSystemSettingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaSystemSettingRepository(jpaRepository);
    }

    @Test
    @DisplayName("保存并往返查询配置")
    void save_roundTrip_mapsAllFields() {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey("audit.retention.days");
        setting.setSettingValue("90");
        setting.setGroupName("AUDIT");
        setting.setValueType("NUMBER");
        setting.setEditable(true);

        SystemSettingDo doObj = new SystemSettingDo();
        doObj.setId(1L);
        doObj.setSettingKey("audit.retention.days");
        doObj.setSettingValue("90");
        doObj.setGroupName("AUDIT");
        doObj.setValueType("NUMBER");
        doObj.setEditable(true);
        when(jpaRepository.save(any())).thenReturn(doObj);

        SystemSetting saved = repository.save(setting);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getSettingKey()).isEqualTo("audit.retention.days");
        assertThat(saved.getSettingValue()).isEqualTo("90");
        assertThat(saved.getGroupName()).isEqualTo("AUDIT");
        assertThat(saved.getValueType()).isEqualTo("NUMBER");
        assertThat(saved.isEditable()).isTrue();
    }

    @Test
    @DisplayName("findByKey 命中与未命中")
    void findByKey_hitAndMiss() {
        SystemSettingDo doObj = new SystemSettingDo();
        doObj.setSettingKey("audit.retention.days");
        doObj.setSettingValue("90");
        when(jpaRepository.findBySettingKey("audit.retention.days")).thenReturn(Optional.of(doObj));
        when(jpaRepository.findBySettingKey("missing")).thenReturn(Optional.empty());

        assertThat(repository.findByKey("audit.retention.days")).isPresent();
        assertThat(repository.findByKey("audit.retention.days").get().getSettingValue()).isEqualTo("90");
        assertThat(repository.findByKey("missing")).isEmpty();
    }
}
