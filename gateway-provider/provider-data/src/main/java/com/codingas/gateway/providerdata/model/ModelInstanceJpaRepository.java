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
package com.codingas.gateway.providerdata.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型实例 Repository
 */
@Repository
public interface ModelInstanceJpaRepository extends JpaRepository<ModelInstanceDo, Long> {

    List<ModelInstanceDo> findByChannelId(Long channelId);

    List<ModelInstanceDo> findByChannelIdAndState(Long channelId, String state);

    List<ModelInstanceDo> findByModelIdAndState(Long modelId, String state);

    List<ModelInstanceDo> findByModelId(Long modelId);

    List<ModelInstanceDo> findByModelIdAndStateOrderByPriorityAsc(Long modelId, String state);

    List<ModelInstanceDo> findByIdIn(List<Long> ids);
}