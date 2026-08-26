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
package com.codingas.gateway.iamdata.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 应用-渠道授权关联 JPA Repository
 */
public interface ApplicationChannelJpaRepository extends JpaRepository<ApplicationChannelDo, Long> {

    /**
     * 查询应用可见的渠道 ID 列表（DISTINCT 去重）
     *
     * @param appId 应用 ID
     * @return 渠道 ID 列表
     */
    @Query("SELECT DISTINCT ac.channelId FROM ApplicationChannelDo ac WHERE ac.applicationId = :appId")
    List<Long> findChannelIdsByApplicationId(@Param("appId") Long appId);

    /**
     * 查询应用下的全部授权关联
     *
     * @param applicationId 应用 ID
     * @return 关联 DO 列表
     */
    List<ApplicationChannelDo> findByApplicationId(Long applicationId);

    /**
     * 删除应用下的全部授权关联
     *
     * @param applicationId 应用 ID
     */
    @Modifying
    @Query("DELETE FROM ApplicationChannelDo ac WHERE ac.applicationId = :applicationId")
    void deleteByApplicationId(@Param("applicationId") Long applicationId);
}
