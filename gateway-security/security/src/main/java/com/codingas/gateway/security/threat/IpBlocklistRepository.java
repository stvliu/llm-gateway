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
package com.codingas.gateway.security.threat;

/**
 * IP 黑名单网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface IpBlocklistRepository {

    /**
     * 检查 IP 是否被封锁
     *
     * @param ipAddress IP 地址
     * @return 是否被封锁
     */
    boolean isBlocked(String ipAddress);

    /**
     * 封锁 IP
     *
     * @param ipAddress IP 地址
     * @param reason 封锁原因
     * @param blockedBy 封锁操作人
     * @param expiresAt 过期时间，null 表示永久封锁
     */
    void block(String ipAddress, String reason, Long blockedBy, java.time.Instant expiresAt);

    /**
     * 解封 IP
     *
     * @param ipAddress IP 地址
     */
    void unblock(String ipAddress);
}