package com.codingas.gateway.infrastructure.template.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinTemplateLoaderTest {

    @Test
    void testLoadBuiltinTemplates() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);

        // 测试不同的路径模式
        String[] patterns = {
            "classpath*:templates/*.json",
            "classpath:templates/*.json",
            "templates/*.json"
        };

        for (String pattern : patterns) {
            System.out.println("Testing pattern: " + pattern);
            try {
                Resource[] resources = resolver.getResources(pattern);
                System.out.println("  Found " + resources.length + " resources");
                for (Resource resource : resources) {
                    System.out.println("  - " + resource.getFilename() + " (" + resource.getURL() + ")");
                    Map<String, Object> template = objectMapper.readValue(resource.getInputStream(), Map.class);
                    System.out.println("    template_code: " + template.get("template_code"));
                }
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }
    }
}
