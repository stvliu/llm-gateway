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

import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统设置数据对象（system_settings 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "system_settings")
public class SystemSettingDo extends BaseDo {

    @Column(name = "setting_key", nullable = false, length = 128)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "text")
    private String settingValue;

    @Column(name = "group_name", length = 64)
    private String groupName;

    @Column(name = "description", length = 256)
    private String description;

    @Column(name = "value_type", length = 32)
    private String valueType;

    @Column(name = "is_editable")
    private boolean editable;
}
