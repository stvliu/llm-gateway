package com.codingas.gateway.infrastructure.metadata.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内置元数据加载器集成测试
 * <p>
 * 使用真实的 classpath 资源验证 JSON 加载。
 * </p>
 */
class BuiltinMetadataLoaderTest {

    private BuiltinMetadataLoader loader;

    @BeforeEach
    void setUp() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        loader = new BuiltinMetadataLoader(resourceLoader);
    }

    @Nested
    @DisplayName("加载供应商元数据")
    class ProviderTests {

        @Test
        @DisplayName("从 classpath 加载所有供应商 JSON")
        void loadsAllProviderMetadata() {
            List<Map<String, Object>> providers = loader.loadProviderMetadata();

            assertThat(providers).isNotEmpty();
            assertThat(providers.size()).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("每个供应商包含 provider_id 字段")
        void eachProviderHasProviderId() {
            List<Map<String, Object>> providers = loader.loadProviderMetadata();

            assertThat(providers)
                .allSatisfy(p -> assertThat(p).containsKey("provider_id"));
        }

        @Test
        @DisplayName("包含 openai 供应商")
        void containsOpenai() {
            List<Map<String, Object>> providers = loader.loadProviderMetadata();

            assertThat(providers)
                .anySatisfy(p -> assertThat(p.get("provider_id")).isEqualTo("openai"));
        }
    }

    @Nested
    @DisplayName("加载模型元数据")
    class ModelTests {

        @Test
        @DisplayName("从 classpath 加载所有模型 JSON")
        void loadsAllModelMetadata() {
            List<Map<String, Object>> models = loader.loadModelMetadata();

            assertThat(models).isNotEmpty();
        }

        @Test
        @DisplayName("每个模型包含 provider_id 字段（从文件名推断）")
        void eachModelHasProviderId() {
            List<Map<String, Object>> models = loader.loadModelMetadata();

            assertThat(models)
                .allSatisfy(m -> assertThat(m).containsKey("provider_id"));
        }

        @Test
        @DisplayName("openai 的模型包含 provider_id=openai")
        void openaiModelsHaveCorrectProviderId() {
            List<Map<String, Object>> models = loader.loadModelMetadata();

            List<Map<String, Object>> openaiModels = models.stream()
                .filter(m -> "openai".equals(m.get("provider_id")))
                .toList();

            assertThat(openaiModels).isNotEmpty();
        }
    }
}