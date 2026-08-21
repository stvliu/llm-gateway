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
package com.codingas.gateway.securitydata.threat;

import com.codingas.gateway.securitydata.threat.IpBlocklistDo;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpBlocklistRepository extends JpaRepository<IpBlocklistDo, Long> {
    Optional<IpBlocklistDo> findByIpAddress(String ipAddress);

    boolean existsByIpAddress(String ipAddress);

    @NotNull
    List<IpBlocklistDo> findAll();

    @NotNull
    Page<IpBlocklistDo> findAll(@NotNull Pageable pageable);

    @NotNull IpBlocklistDo save(@NotNull IpBlocklistDo blocklist);

    void delete(@NotNull IpBlocklistDo blocklist);
}
