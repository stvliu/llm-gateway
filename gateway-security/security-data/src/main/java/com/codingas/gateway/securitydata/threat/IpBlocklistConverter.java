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

import com.codingas.gateway.security.threat.IpBlocklist;
import com.codingas.gateway.securitydata.threat.IpBlocklistDo;
import org.springframework.stereotype.Component;

/**
 * IP 黑名单对象转换器
 *
 * <p>负责在实体 {@link IpBlocklist} 和数据对象 {@link IpBlocklistDo} 之间进行转换。</p>
 */
@Component
public class IpBlocklistConverter {

    /**
     * 将数据对象转换为实体
     *
     * @param ipBlocklistDo 数据对象
     * @return 实体
     */
    public IpBlocklist toDomain(IpBlocklistDo ipBlocklistDo) {
        if (ipBlocklistDo == null) {
            return null;
        }

        IpBlocklist ipBlocklist = new IpBlocklist();
        ipBlocklist.setId(ipBlocklistDo.getId());
        ipBlocklist.setIpAddress(ipBlocklistDo.getIpAddress());
        ipBlocklist.setBlockReason(ipBlocklistDo.getBlockReason());
        ipBlocklist.setBlockedAt(ipBlocklistDo.getBlockedAt());
        ipBlocklist.setExpiresAt(ipBlocklistDo.getExpiresAt());
        ipBlocklist.setBlockedBy(ipBlocklistDo.getBlockedBy());
        ipBlocklist.setCreatedAt(ipBlocklistDo.getCreatedAt());
        ipBlocklist.setUpdatedAt(ipBlocklistDo.getUpdatedAt());

        return ipBlocklist;
    }

    /**
     * 将实体转换为数据对象
     *
     * @param ipBlocklist 实体
     * @return 数据对象
     */
    public IpBlocklistDo toDataObject(IpBlocklist ipBlocklist) {
        if (ipBlocklist == null) {
            return null;
        }

        IpBlocklistDo ipBlocklistDo = new IpBlocklistDo();
        ipBlocklistDo.setId(ipBlocklist.getId());
        ipBlocklistDo.setIpAddress(ipBlocklist.getIpAddress());
        ipBlocklistDo.setBlockReason(ipBlocklist.getBlockReason());
        ipBlocklistDo.setBlockedAt(ipBlocklist.getBlockedAt());
        ipBlocklistDo.setExpiresAt(ipBlocklist.getExpiresAt());
        ipBlocklistDo.setBlockedBy(ipBlocklist.getBlockedBy());
        ipBlocklistDo.setCreatedAt(ipBlocklist.getCreatedAt());
        ipBlocklistDo.setUpdatedAt(ipBlocklist.getUpdatedAt());

        return ipBlocklistDo;
    }

}
