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
package com.codingas.gateway.domain.application.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApplicationChannel 应用-渠道授权关联实体单元测试
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>业务构造器（applicationId, channelId）正确赋值</li>
 *   <li>无参构造器 + Setter 能读写全部 6 个字段（含继承自 BaseEntity 的 id 与审计字段）</li>
 *   <li>无参构造器初始状态所有字段为 null</li>
 * </ul>
 */
@DisplayName("ApplicationChannel 应用-渠道授权关联实体测试")
class ApplicationChannelTest {

    @Nested
    @DisplayName("业务构造器测试")
    class BusinessConstructorTests {

        @Test
        @DisplayName("业务构造器（applicationId, channelId）能正确赋值业务字段")
        void businessConstructor_setsBusinessFields() {
            ApplicationChannel channel = new ApplicationChannel(10L, 20L);

            assertThat(channel.getApplicationId()).isEqualTo(10L);
            assertThat(channel.getChannelId()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("字段读写测试")
    class FieldReadWriteTests {

        @Test
        @DisplayName("无参构造器 + Setter 能读写全部 6 个字段（含 id 与审计字段）")
        void noArgsConstructor_andSetters_readWriteAllFields() {
            ApplicationChannel channel = new ApplicationChannel();
            Instant now = Instant.now();

            channel.setId(1L);
            channel.setApplicationId(100L);
            channel.setChannelId(200L);
            channel.setCreatedBy(999L);
            channel.setCreatedAt(now);
            channel.setUpdatedBy(888L);
            channel.setUpdatedAt(now);

            assertThat(channel.getId()).isEqualTo(1L);
            assertThat(channel.getApplicationId()).isEqualTo(100L);
            assertThat(channel.getChannelId()).isEqualTo(200L);
            assertThat(channel.getCreatedBy()).isEqualTo(999L);
            assertThat(channel.getCreatedAt()).isEqualTo(now);
            assertThat(channel.getUpdatedBy()).isEqualTo(888L);
            assertThat(channel.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("无参构造器初始状态所有字段为 null")
        void noArgsConstructor_allFieldsNullByDefault() {
            ApplicationChannel channel = new ApplicationChannel();

            assertThat(channel.getId()).isNull();
            assertThat(channel.getApplicationId()).isNull();
            assertThat(channel.getChannelId()).isNull();
            assertThat(channel.getCreatedBy()).isNull();
            assertThat(channel.getCreatedAt()).isNull();
            assertThat(channel.getUpdatedBy()).isNull();
            assertThat(channel.getUpdatedAt()).isNull();
        }
    }
}
