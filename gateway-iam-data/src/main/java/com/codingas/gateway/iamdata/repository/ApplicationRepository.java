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
package com.codingas.gateway.iamdata.repository;

import com.codingas.gateway.iamdata.dataobject.ApplicationDo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 应用 JPA Repository
 */
public interface ApplicationRepository extends JpaRepository<ApplicationDo, Long> {

    /**
     * 按应用编码查找
     *
     * @param code 应用编码
     * @return 命中的 DO；不存在时返回空
     */
    Optional<ApplicationDo> findByCode(String code);
}
