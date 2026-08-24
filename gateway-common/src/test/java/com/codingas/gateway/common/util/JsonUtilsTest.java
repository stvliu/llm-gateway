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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonUtils 单元测试
 *
 * <p>覆盖序列化/反序列化/JsonNode/Map 转换/ObjectMapper 注入与重置，以及异常与空输入分支。</p>
 */
@DisplayName("JsonUtils 测试")
class JsonUtilsTest {

    @BeforeEach
    void setUp() {
        // 保证每个用例从干净状态开始
        JsonUtils.reset();
    }

    @AfterEach
    void tearDown() {
        JsonUtils.reset();
    }

    @Nested
    @DisplayName("ObjectMapper 管理")
    class ObjectMapperManagementTests {

        @Test
        @DisplayName("默认 ObjectMapper 为延迟初始化的默认配置实例")
        void getObjectMapper_lazyDefault_instanceCached() {
            // when
            ObjectMapper first = JsonUtils.getObjectMapper();
            ObjectMapper second = JsonUtils.getObjectMapper();

            // then
            assertThat(first).isNotNull();
            assertThat(second).isSameAs(first);
        }

        @Test
        @DisplayName("setObjectMapper 注入自定义实例且可被 getObjectMapper 获取")
        void setObjectMapper_customInstance_returned() {
            // given
            ObjectMapper custom = new ObjectMapper();

            // when
            JsonUtils.setObjectMapper(custom);

            // then
            assertThat(JsonUtils.getObjectMapper()).isSameAs(custom);
        }

        @Test
        @DisplayName("reset 后重新获取默认实例")
        void reset_afterCustom_returnsNewDefault() {
            // given
            JsonUtils.setObjectMapper(new ObjectMapper());

            // when
            JsonUtils.reset();

            // then
            ObjectMapper fresh = JsonUtils.getObjectMapper();
            assertThat(fresh).isNotNull();
            assertThat(fresh.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        }

        @Test
        @DisplayName("默认 ObjectMapper 对齐 application.yml 三项配置")
        void defaultMapper_featuresConfigured() {
            // when
            ObjectMapper mapper = JsonUtils.getObjectMapper();

            // then
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
            assertThat(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        }
    }

    @Nested
    @DisplayName("序列化")
    class SerializationTests {

        @Test
        @DisplayName("toJson 序列化普通对象")
        void toJson_validObject_returnsJson() {
            // given - 保证字段顺序可断言
            Map<String, Object> input = new java.util.LinkedHashMap<>();
            input.put("name", "gateway");
            input.put("port", 8080);

            // when
            String json = JsonUtils.toJson(input);

            // then
            assertThat(json).isEqualTo("{\"name\":\"gateway\",\"port\":8080}");
        }

        @Test
        @DisplayName("toJson 入参为 null 返回 null")
        void toJson_null_returnsNull() {
            assertThat(JsonUtils.toJson(null)).isNull();
        }

        @Test
        @DisplayName("toJson 序列化失败返回 null（空 Bean + FAIL_ON_EMPTY_BEANS 开启的 mapper）")
        void toJson_serializationFailure_returnsNull() {
            // given
            JsonUtils.setObjectMapper(new ObjectMapper()); // 默认 FAIL_ON_EMPTY_BEANS=true

            // when
            String result = JsonUtils.toJson(new EmptyBean());

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("toJsonPretty 美化输出")
        void toJsonPretty_validObject_prettyJson() {
            // when
            String json = JsonUtils.toJsonPretty(Map.of("a", 1));

            // then
            assertThat(json).contains("\"a\" : 1");
        }

        @Test
        @DisplayName("toJsonPretty 入参为 null 返回 null")
        void toJsonPretty_null_returnsNull() {
            assertThat(JsonUtils.toJsonPretty(null)).isNull();
        }

        @Test
        @DisplayName("toJsonPretty 序列化失败返回 null")
        void toJsonPretty_failure_returnsNull() {
            // given
            JsonUtils.setObjectMapper(new ObjectMapper());

            // when
            String result = JsonUtils.toJsonPretty(new EmptyBean());

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("默认 mapper 禁用 WRITE_DATES_AS_TIMESTAMPS，Date 序列化为字符串")
        void toJson_date_serializedAsString() {
            // given
            Date date = new Date(0L);

            // when
            String json = JsonUtils.toJson(Map.of("ts", date));

            // then
            assertThat(json).startsWith("{\"ts\":\"");
        }
    }

    @Nested
    @DisplayName("反序列化（字符串）")
    class FromJsonStringTests {

        @Test
        @DisplayName("fromJson 反序列化为指定类型")
        void fromJson_validJson_returnsObject() {
            // when
            SampleDto dto = JsonUtils.fromJson("{\"name\":\"gw\",\"count\":3}", SampleDto.class);

            // then
            assertThat(dto).isNotNull();
            assertThat(dto.getName()).isEqualTo("gw");
            assertThat(dto.getCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("fromJson 空串或 null 返回 null")
        void fromJson_blank_returnsNull() {
            assertThat(JsonUtils.fromJson((String) null, SampleDto.class)).isNull();
            assertThat(JsonUtils.fromJson("", SampleDto.class)).isNull();
        }

        @Test
        @DisplayName("fromJson 非法 JSON 返回 null")
        void fromJson_invalidJson_returnsNull() {
            assertThat(JsonUtils.fromJson("{oops", SampleDto.class)).isNull();
        }

        @Test
        @DisplayName("fromJson 多余未知字段被忽略（FAIL_ON_UNKNOWN_PROPERTIES 关闭）")
        void fromJson_unknownProperty_ignored() {
            // when
            SampleDto dto = JsonUtils.fromJson("{\"name\":\"gw\",\"unknown\":1}", SampleDto.class);

            // then
            assertThat(dto).isNotNull();
            assertThat(dto.getName()).isEqualTo("gw");
        }

        @Test
        @DisplayName("fromJson 泛型 TypeReference 反序列化集合")
        void fromJson_typeReference_list() {
            // when
            List<SampleDto> list = JsonUtils.fromJson(
                    "[{\"name\":\"a\"},{\"name\":\"b\"}]",
                    new TypeReference<List<SampleDto>>() {});

            // then
            assertThat(list).hasSize(2);
            assertThat(list.get(0).getName()).isEqualTo("a");
            assertThat(list.get(1).getName()).isEqualTo("b");
        }

        @Test
        @DisplayName("fromJson TypeReference 空串或 null 返回 null")
        void fromJson_typeReference_blank_returnsNull() {
            assertThat(JsonUtils.fromJson((String) null, new TypeReference<List<SampleDto>>() {})).isNull();
            assertThat(JsonUtils.fromJson("", new TypeReference<List<SampleDto>>() {})).isNull();
        }

        @Test
        @DisplayName("fromJson TypeReference 非法 JSON 返回 null")
        void fromJson_typeReference_invalid_returnsNull() {
            assertThat(JsonUtils.fromJson("{bad", new TypeReference<List<SampleDto>>() {})).isNull();
        }
    }

    @Nested
    @DisplayName("反序列化（输入流）")
    class FromJsonStreamTests {

        @Test
        @DisplayName("fromJson 输入流反序列化为指定类型")
        void fromJson_stream_valid_returnsObject() {
            // given
            InputStream in = stream("{\"name\":\"gw\",\"count\":5}");

            // when
            SampleDto dto = JsonUtils.fromJson(in, SampleDto.class);

            // then
            assertThat(dto).isNotNull();
            assertThat(dto.getName()).isEqualTo("gw");
            assertThat(dto.getCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("fromJson 输入流为 null 返回 null")
        void fromJson_stream_null_returnsNull() {
            assertThat(JsonUtils.fromJson((InputStream) null, SampleDto.class)).isNull();
        }

        @Test
        @DisplayName("fromJson 输入流非法 JSON 返回 null")
        void fromJson_stream_invalid_returnsNull() {
            assertThat(JsonUtils.fromJson(stream("{bad"), SampleDto.class)).isNull();
        }

        @Test
        @DisplayName("fromJson 输入流 + TypeReference 反序列化集合")
        void fromJson_stream_typeReference_valid() {
            // given
            InputStream in = stream("[{\"name\":\"a\"}]");

            // when
            List<SampleDto> list = JsonUtils.fromJson(in, new TypeReference<List<SampleDto>>() {});

            // then
            assertThat(list).hasSize(1);
            assertThat(list.get(0).getName()).isEqualTo("a");
        }

        @Test
        @DisplayName("fromJson 输入流 + TypeReference 为 null 返回 null")
        void fromJson_stream_typeReference_null_returnsNull() {
            assertThat(JsonUtils.fromJson((InputStream) null, new TypeReference<List<SampleDto>>() {})).isNull();
        }

        @Test
        @DisplayName("fromJson 输入流 + TypeReference 非法 JSON 返回 null")
        void fromJson_stream_typeReference_invalid_returnsNull() {
            assertThat(JsonUtils.fromJson(stream("{bad"), new TypeReference<List<SampleDto>>() {})).isNull();
        }
    }

    @Nested
    @DisplayName("JsonNode 与 Map 转换")
    class JsonNodeAndMapTests {

        @Test
        @DisplayName("readTree 解析合法 JSON")
        void readTree_valid_returnsNode() {
            // when
            JsonNode node = JsonUtils.readTree("{\"a\":{\"b\":1}}");

            // then
            assertThat(node).isNotNull();
            assertThat(node.path("a").path("b").asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("readTree 空串或 null 返回 null")
        void readTree_blank_returnsNull() {
            assertThat(JsonUtils.readTree(null)).isNull();
            assertThat(JsonUtils.readTree("")).isNull();
        }

        @Test
        @DisplayName("readTree 非法 JSON 返回 null")
        void readTree_invalid_returnsNull() {
            assertThat(JsonUtils.readTree("{nope")).isNull();
        }

        @Test
        @DisplayName("toMap 字符串转 Map")
        void toMap_string_valid_returnsMap() {
            // when
            Map<String, Object> map = JsonUtils.toMap("{\"x\":1,\"y\":\"z\"}");

            // then
            assertThat(map).containsEntry("x", 1).containsEntry("y", "z");
        }

        @Test
        @DisplayName("toMap 字符串空串或 null 返回空 Map")
        void toMap_string_blank_returnsEmpty() {
            assertThat(JsonUtils.toMap((String) null)).isEmpty();
            assertThat(JsonUtils.toMap("")).isEmpty();
        }

        @Test
        @DisplayName("toMap 字符串非法 JSON 返回空 Map")
        void toMap_string_invalid_returnsEmpty() {
            assertThat(JsonUtils.toMap("{bad")).isEmpty();
        }

        @Test
        @DisplayName("toMap 对象转 Map")
        void toMap_object_valid_returnsMap() {
            // when
            Map<String, Object> map = JsonUtils.toMap(new SampleDto("gw", 2));

            // then
            assertThat(map).containsEntry("name", "gw").containsEntry("count", 2);
        }

        @Test
        @DisplayName("toMap 对象为 null 返回空 Map")
        void toMap_object_null_returnsEmpty() {
            assertThat(JsonUtils.toMap((Object) null)).isEmpty();
        }

        @Test
        @DisplayName("toMap 对象序列化失败返回空 Map")
        void toMap_object_invalid_returnsEmpty() {
            // given
            JsonUtils.setObjectMapper(new ObjectMapper());

            // when
            Map<String, Object> map = JsonUtils.toMap(new EmptyBean());

            // then
            assertThat(map).isEmpty();
        }
    }

    // Helper 类与方法
    private InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /** 无字段 Bean，配合默认 FAIL_ON_EMPTY_BEANS=true 的 ObjectMapper 触发序列化失败 */
    static class EmptyBean {
    }

    /** 反序列化测试用简单 DTO */
    static class SampleDto {
        private String name;
        private int count;

        SampleDto() {
        }

        SampleDto(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
