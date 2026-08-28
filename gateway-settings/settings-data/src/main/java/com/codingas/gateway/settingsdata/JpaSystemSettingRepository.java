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
import com.codingas.gateway.settings.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 系统设置持久化实现（实体 ↔ DO 双向转换，含审计字段）
 */
@Component
@RequiredArgsConstructor
public class JpaSystemSettingRepository implements SystemSettingRepository {

    private final SystemSettingJpaRepository jpaRepository;

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        return jpaRepository.findBySettingKey(key).map(this::toEntity);
    }

    @Override
    public List<SystemSetting> findAll() {
        return jpaRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public SystemSetting save(SystemSetting setting) {
        SystemSettingDo doObj = toDo(setting);
        SystemSettingDo saved = jpaRepository.save(doObj);
        return toEntity(saved);
    }

    private SystemSetting toEntity(SystemSettingDo doObj) {
        SystemSetting entity = new SystemSetting();
        entity.setId(doObj.getId());
        entity.setSettingKey(doObj.getSettingKey());
        entity.setSettingValue(doObj.getSettingValue());
        entity.setGroupName(doObj.getGroupName());
        entity.setDescription(doObj.getDescription());
        entity.setValueType(doObj.getValueType());
        entity.setEditable(doObj.isEditable());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private SystemSettingDo toDo(SystemSetting entity) {
        SystemSettingDo doObj = new SystemSettingDo();
        doObj.setId(entity.getId());
        doObj.setSettingKey(entity.getSettingKey());
        doObj.setSettingValue(entity.getSettingValue());
        doObj.setGroupName(entity.getGroupName());
        doObj.setDescription(entity.getDescription());
        doObj.setValueType(entity.getValueType());
        doObj.setEditable(entity.isEditable());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        doObj.setCreatedAt(entity.getCreatedAt());
        doObj.setUpdatedAt(entity.getUpdatedAt());
        return doObj;
    }
}
