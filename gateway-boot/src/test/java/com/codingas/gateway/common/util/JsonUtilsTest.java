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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonUtils 单元测试
 */
class JsonUtilsTest {

    @AfterEach
    void tearDown() {
        // 每个测试后重置，避免影响其他测试
        JsonUtils.reset();
    }

    // ==================== 测试数据类 ====================

    record TestUser(Long id, String name, Integer age) {}

    record TestOrder(String orderId, String customer, Double amount) {}

    // ==================== getObjectMapper 测试 ====================

    @Nested
    @DisplayName("getObjectMapper 测试")
    class GetObjectMapperTest {

        @Test
        @DisplayName("未设置时返回默认 ObjectMapper")
        void withoutSetting_returnsDefaultObjectMapper() {
            ObjectMapper mapper = JsonUtils.getObjectMapper();

            assertThat(mapper).isNotNull();
            // 验证默认配置
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
            assertThat(mapper.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isFalse();
            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        }

        @Test
        @DisplayName("设置后返回设置的 ObjectMapper")
        void afterSetting_returnsSetObjectMapper() {
            ObjectMapper customMapper = new ObjectMapper();
            JsonUtils.setObjectMapper(customMapper);

            ObjectMapper mapper = JsonUtils.getObjectMapper();

            assertThat(mapper).isSameAs(customMapper);
        }

        @Test
        @DisplayName("多线程并发获取 ObjectMapper 安全")
        void concurrentAccess_isThreadSafe() throws InterruptedException {
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        ObjectMapper mapper = JsonUtils.getObjectMapper();
                        if (mapper != null) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(threadCount);
        }
    }

    // ==================== toJson 测试 ====================

    @Nested
    @DisplayName("toJson 测试")
    class ToJsonTest {

        @Test
        @DisplayName("序列化普通对象")
        void normalObject_serializesCorrectly() {
            TestUser user = new TestUser(1L, "张三", 25);

            String json = JsonUtils.toJson(user);

            assertThat(json).isNotNull();
            assertThat(json).contains("\"id\":1");
            assertThat(json).contains("\"name\":\"张三\"");
            assertThat(json).contains("\"age\":25");
        }

        @Test
        @DisplayName("序列化 null 返回 null")
        void nullObject_returnsNull() {
            String json = JsonUtils.toJson(null);

            assertThat(json).isNull();
        }

        @Test
        @DisplayName("序列化空字符串")
        void emptyString_returnsQuotes() {
            String json = JsonUtils.toJson("");

            assertThat(json).isEqualTo("\"\"");
        }

        @Test
        @DisplayName("序列化 Map")
        void mapObject_serializesCorrectly() {
            Map<String, Object> map = Map.of("key1", "value1", "key2", 123);

            String json = JsonUtils.toJson(map);

            assertThat(json).isNotNull();
            assertThat(json).contains("\"key1\":\"value1\"");
            assertThat(json).contains("\"key2\":123");
        }

        @Test
        @DisplayName("序列化 List")
        void listObject_serializesCorrectly() {
            List<String> list = List.of("a", "b", "c");

            String json = JsonUtils.toJson(list);

            assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
        }
    }

    // ==================== toJsonPretty 测试 ====================

    @Nested
    @DisplayName("toJsonPretty 测试")
    class ToJsonPrettyTest {

        @Test
        @DisplayName("美化输出包含换行")
        void prettyOutput_containsNewlines() {
            TestUser user = new TestUser(1L, "张三", 25);

            String json = JsonUtils.toJsonPretty(user);

            assertThat(json).isNotNull();
            assertThat(json).contains("\n");
            assertThat(json).contains("  "); // 缩进
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput_returnsNull() {
            String json = JsonUtils.toJsonPretty(null);

            assertThat(json).isNull();
        }
    }

    // ==================== fromJson(String, Class) 测试 ====================

    @Nested
    @DisplayName("fromJson(String, Class) 测试")
    class FromJsonStringClassTest {

        @Test
        @DisplayName("正常反序列化")
        void validJson_deserializesCorrectly() {
            String json = "{\"id\":1,\"name\":\"张三\",\"age\":25}";

            TestUser user = JsonUtils.fromJson(json, TestUser.class);

            assertThat(user).isNotNull();
            assertThat(user.id()).isEqualTo(1L);
            assertThat(user.name()).isEqualTo("张三");
            assertThat(user.age()).isEqualTo(25);
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput_returnsNull() {
            TestUser user = JsonUtils.fromJson(null, TestUser.class);

            assertThat(user).isNull();
        }

        @Test
        @DisplayName("空字符串返回 null")
        void emptyString_returnsNull() {
            TestUser user = JsonUtils.fromJson("", TestUser.class);

            assertThat(user).isNull();
        }

        @Test
        @DisplayName("无效 JSON 返回 null")
        void invalidJson_returnsNull() {
            String invalidJson = "{invalid json}";

            TestUser user = JsonUtils.fromJson(invalidJson, TestUser.class);

            assertThat(user).isNull();
        }

        @Test
        @DisplayName("未知属性不报错（配置生效）")
        void unknownProperties_ignored() {
            String json = "{\"id\":1,\"name\":\"张三\",\"age\":25,\"unknown\":\"value\"}";

            TestUser user = JsonUtils.fromJson(json, TestUser.class);

            assertThat(user).isNotNull();
            assertThat(user.id()).isEqualTo(1L);
        }
    }

    // ==================== fromJson(String, TypeReference) 测试 ====================

    @Nested
    @DisplayName("fromJson(String, TypeReference) 测试")
    class FromJsonStringTypeReferenceTest {

        @Test
        @DisplayName("反序列化泛型 List")
        void genericList_deserializesCorrectly() {
            String json = "[\"a\",\"b\",\"c\"]";

            List<String> list = JsonUtils.fromJson(json, new TypeReference<List<String>>() {});

            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("反序列化泛型 Map")
        void genericMap_deserializesCorrectly() {
            String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";

            Map<String, String> map = JsonUtils.fromJson(json, new TypeReference<Map<String, String>>() {});

            assertThat(map).containsEntry("key1", "value1");
            assertThat(map).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("反序列化复杂泛型")
        void complexGeneric_deserializesCorrectly() {
            String json = "[{\"id\":1,\"name\":\"张三\",\"age\":25}]";

            List<TestUser> users = JsonUtils.fromJson(json, new TypeReference<List<TestUser>>() {});

            assertThat(users).hasSize(1);
            assertThat(users.get(0).name()).isEqualTo("张三");
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput_returnsNull() {
            List<String> list = JsonUtils.fromJson(null, new TypeReference<List<String>>() {});

            assertThat(list).isNull();
        }
    }

    // ==================== fromJson(InputStream, Class) 测试 ====================

    @Nested
    @DisplayName("fromJson(InputStream, Class) 测试")
    class FromJsonInputStreamClassTest {

        @Test
        @DisplayName("从输入流反序列化")
        void fromInputStream_deserializesCorrectly() {
            String json = "{\"id\":1,\"name\":\"张三\",\"age\":25}";
            InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

            TestUser user = JsonUtils.fromJson(inputStream, TestUser.class);

            assertThat(user).isNotNull();
            assertThat(user.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("null 输入流返回 null")
        void nullInputStream_returnsNull() {
            TestUser user = JsonUtils.fromJson((InputStream) null, TestUser.class);

            assertThat(user).isNull();
        }
    }

    // ==================== fromJson(InputStream, TypeReference) 测试 ====================

    @Nested
    @DisplayName("fromJson(InputStream, TypeReference) 测试")
    class FromJsonInputStreamTypeReferenceTest {

        @Test
        @DisplayName("从输入流反序列化泛型")
        void fromInputStreamGeneric_deserializesCorrectly() {
            String json = "[{\"id\":1,\"name\":\"张三\",\"age\":25}]";
            InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

            List<TestUser> users = JsonUtils.fromJson(inputStream, new TypeReference<List<TestUser>>() {});

            assertThat(users).hasSize(1);
            assertThat(users.get(0).name()).isEqualTo("张三");
        }
    }

    // ==================== readTree 测试 ====================

    @Nested
    @DisplayName("readTree 测试")
    class ReadTreeTest {

        @Test
        @DisplayName("解析 JSON 树")
        void validJson_parsesToTree() {
            String json = "{\"name\":\"张三\",\"age\":25}";

            JsonNode node = JsonUtils.readTree(json);

            assertThat(node).isNotNull();
            assertThat(node.get("name").asText()).isEqualTo("张三");
            assertThat(node.get("age").asInt()).isEqualTo(25);
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput_returnsNull() {
            JsonNode node = JsonUtils.readTree(null);

            assertThat(node).isNull();
        }

        @Test
        @DisplayName("无效 JSON 返回 null")
        void invalidJson_returnsNull() {
            JsonNode node = JsonUtils.readTree("{invalid}");

            assertThat(node).isNull();
        }

        @Test
        @DisplayName("解析嵌套 JSON")
        void nestedJson_parsesCorrectly() {
            String json = "{\"user\":{\"name\":\"张三\"},\"items\":[1,2,3]}";

            JsonNode node = JsonUtils.readTree(json);

            assertThat(node.get("user").get("name").asText()).isEqualTo("张三");
            assertThat(node.get("items").get(1).asInt()).isEqualTo(2);
        }
    }

    // ==================== toMap(String) 测试 ====================

    @Nested
    @DisplayName("toMap(String) 测试")
    class ToMapStringTest {

        @Test
        @DisplayName("JSON 字符串转 Map")
        void jsonString_convertsToMap() {
            String json = "{\"key1\":\"value1\",\"key2\":123}";

            Map<String, Object> map = JsonUtils.toMap(json);

            assertThat(map).containsEntry("key1", "value1");
            assertThat(map).containsEntry("key2", 123);
        }

        @Test
        @DisplayName("null 输入返回空 Map")
        void nullInput_returnsEmptyMap() {
            Map<String, Object> map = JsonUtils.toMap((String) null);

            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("空字符串返回空 Map")
        void emptyString_returnsEmptyMap() {
            Map<String, Object> map = JsonUtils.toMap("");

            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("无效 JSON 返回空 Map")
        void invalidJson_returnsEmptyMap() {
            Map<String, Object> map = JsonUtils.toMap("{invalid}");

            assertThat(map).isEmpty();
        }
    }

    // ==================== toMap(Object) 测试 ====================

    @Nested
    @DisplayName("toMap(Object) 测试")
    class ToMapObjectTest {

        @Test
        @DisplayName("对象转 Map")
        void object_convertsToMap() {
            TestUser user = new TestUser(1L, "张三", 25);

            Map<String, Object> map = JsonUtils.toMap(user);

            assertThat(map).containsEntry("id", 1);
            assertThat(map).containsEntry("name", "张三");
            assertThat(map).containsEntry("age", 25);
        }

        @Test
        @DisplayName("null 对象返回空 Map")
        void nullObject_returnsEmptyMap() {
            Map<String, Object> map = JsonUtils.toMap((Object) null);

            assertThat(map).isEmpty();
        }

        @Test
        @DisplayName("Map 对象直接转换")
        void mapObject_convertsCorrectly() {
            Map<String, Object> original = Map.of("key", "value", "num", 42);

            Map<String, Object> map = JsonUtils.toMap((Object) original);

            assertThat(map).containsEntry("key", "value");
            assertThat(map).containsEntry("num", 42);
        }
    }

    // ==================== Spring 模式测试 ====================

    @Nested
    @DisplayName("Spring 注入模式测试")
    class SpringInjectionTest {

        @Test
        @DisplayName("设置自定义 ObjectMapper 后使用自定义配置")
        void afterSettingCustomMapper_usesCustomConfig() {
            // 创建自定义配置的 ObjectMapper
            ObjectMapper customMapper = new ObjectMapper();
            // 启用 WRITE_DATES_AS_TIMESTAMPS（与默认配置相反）
            customMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            JsonUtils.setObjectMapper(customMapper);

            // 验证使用的是自定义的 ObjectMapper
            assertThat(JsonUtils.getObjectMapper()).isSameAs(customMapper);
            assertThat(JsonUtils.getObjectMapper().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isTrue();
        }

        @Test
        @DisplayName("重复设置 ObjectMapper 后使用最新设置")
        void repeatedSetting_usesLatestSetting() {
            ObjectMapper firstMapper = new ObjectMapper();
            ObjectMapper secondMapper = new ObjectMapper();

            JsonUtils.setObjectMapper(firstMapper);
            assertThat(JsonUtils.getObjectMapper()).isSameAs(firstMapper);

            JsonUtils.setObjectMapper(secondMapper);
            assertThat(JsonUtils.getObjectMapper()).isSameAs(secondMapper);
        }
    }

    // ==================== reset 测试 ====================

    @Nested
    @DisplayName("reset 测试")
    class ResetTest {

        @Test
        @DisplayName("reset 后返回默认 ObjectMapper")
        void afterReset_returnsDefaultObjectMapper() {
            ObjectMapper customMapper = new ObjectMapper();
            JsonUtils.setObjectMapper(customMapper);

            JsonUtils.reset();

            assertThat(JsonUtils.getObjectMapper()).isNotSameAs(customMapper);
            // 验证是默认配置
            assertThat(JsonUtils.getObjectMapper().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        }
    }
}
