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
package com.codingas.gateway.providerdata.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "providers")
public class ProviderDo extends BaseDo {

    @Column(name = "provider_id", unique = true, length = 64)
    private String code;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String name;

    @Column(name = "icon_url", length = 512)
    private String logoUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "description", length = 512)
    private String description;
}