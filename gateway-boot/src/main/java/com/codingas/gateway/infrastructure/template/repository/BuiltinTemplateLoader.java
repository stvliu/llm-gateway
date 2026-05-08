package com.codingas.gateway.infrastructure.template.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 内置模板资源加载器
 *
 * <p>从 classpath:templates/ 目录加载预置的供应商模板。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinTemplateLoader {

    private static final String TEMPLATES_LOCATION = "classpath*:templates/*.json";

    private final ObjectMapper objectMapper;

    private final ResourceLoader resourceLoader;

    /**
     * 加载所有内置模板
     *
     * @return 模板数据列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadBuiltinTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources(TEMPLATES_LOCATION);
            log.info("Found {} builtin template files from {}", resources.length, TEMPLATES_LOCATION);

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null) {
                        continue;
                    }

                    log.debug("Loading template file: {}", filename);
                    Map<String, Object> template = objectMapper.readValue(
                        resource.getInputStream(),
                        Map.class
                    );
                    templates.add(template);
                    log.debug("Loaded builtin template: {} from {}", template.get("template_code"), filename);

                } catch (Exception e) {
                    log.error("Failed to load template from: {}", resource.getFilename(), e);
                }
            }

            log.info("Successfully loaded {} builtin templates", templates.size());

        } catch (IOException e) {
            log.warn("Failed to resolve template resources from classpath", e);
        }

        return templates;
    }
}
