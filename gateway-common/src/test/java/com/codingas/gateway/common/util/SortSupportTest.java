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
package com.codingas.gateway.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SortSupport 排序助手单元测试
 */
@DisplayName("SortSupport 排序助手")
class SortSupportTest {

    private static final Set<String> ALLOWED = Set.of("id", "name", "state");

    @Test
    @DisplayName("normalize 白名单内字段原样返回")
    void normalize_allowedField_returnsItself() {
        assertThat(SortSupport.normalize("name", ALLOWED, "id")).isEqualTo("name");
    }

    @Test
    @DisplayName("normalize 非法字段回退默认（防注入）")
    void normalize_invalidField_fallsBackToDefault() {
        assertThat(SortSupport.normalize("name; DROP TABLE channels", ALLOWED, "id"))
                .isEqualTo("id");
    }

    @Test
    @DisplayName("normalize 空值回退默认")
    void normalize_blank_fallsBackToDefault() {
        assertThat(SortSupport.normalize(null, ALLOWED, "id")).isEqualTo("id");
        assertThat(SortSupport.normalize("  ", ALLOWED, "id")).isEqualTo("id");
    }

    @Test
    @DisplayName("isDesc 仅识别 DESC（大小写不敏感）")
    void isDesc_recognizesDescIgnoringCase() {
        assertThat(SortSupport.isDesc("DESC")).isTrue();
        assertThat(SortSupport.isDesc("desc")).isTrue();
        assertThat(SortSupport.isDesc("ASC")).isFalse();
        assertThat(SortSupport.isDesc("random")).isFalse();
        assertThat(SortSupport.isDesc(null)).isFalse();
    }

    @Test
    @DisplayName("byString 字母序（大小写不敏感）且 null 值垫底")
    void byString_sortsCaseInsensitiveWithNullLast() {
        Comparator<String> c = SortSupport.byString(s -> s, false);
        List<String> sorted = Arrays.asList("GPT-4", "claude", null, "gemini").stream()
                .sorted(c)
                .toList();
        assertThat(sorted).containsExactly("claude", "gemini", "GPT-4", null);
    }

    @Test
    @DisplayName("byString 降序反转")
    void byString_descReversesOrder() {
        Comparator<String> c = SortSupport.byString(s -> s, true);
        List<String> sorted = List.of("b", "a", "c").stream().sorted(c).toList();
        assertThat(sorted).containsExactly("c", "b", "a");
    }

    @Test
    @DisplayName("byLong 数值序")
    void byLong_sortsNumerically() {
        Comparator<Long> c = SortSupport.byLong(v -> v, false);
        List<Long> sorted = List.of(30L, 10L, 20L).stream().sorted(c).toList();
        assertThat(sorted).containsExactly(10L, 20L, 30L);
    }

    @Test
    @DisplayName("byLong 降序反转")
    void byLong_descReversesOrder() {
        Comparator<Long> c = SortSupport.byLong(v -> v, true);
        List<Long> sorted = List.of(30L, 10L, 20L).stream().sorted(c).toList();
        assertThat(sorted).containsExactly(30L, 20L, 10L);
    }
}
