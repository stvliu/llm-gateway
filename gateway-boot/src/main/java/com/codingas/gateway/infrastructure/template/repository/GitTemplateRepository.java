package com.codingas.gateway.infrastructure.template.repository;

import com.codingas.gateway.infrastructure.template.config.TemplateSyncConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Git 模板仓库操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitTemplateRepository {

    private final TemplateSyncConfig config;
    private final ObjectMapper objectMapper;

    /**
     * 克隆或拉取远程仓库
     */
    public void syncRepository() throws GitAPIException, IOException {
        File repoDir = new File(config.getLocalPath());

        if (repoDir.exists()) {
            log.info("Pulling template repository from {}", config.getUrl());
            try (Git git = Git.open(repoDir)) {
                git.pull().call();
            }
        } else {
            log.info("Cloning template repository from {}", config.getUrl());
            repoDir.getParentFile().mkdirs();
            Git.cloneRepository()
                .setURI(config.getUrl())
                .setDirectory(repoDir)
                .setBranch(config.getBranch())
                .call()
                .close();
        }
    }

    /**
     * 读取所有模板文件
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> loadTemplates() throws IOException {
        List<Map<String, Object>> templates = new ArrayList<>();
        File templatesDir = new File(config.getLocalPath(), "templates");

        if (!templatesDir.exists() || !templatesDir.isDirectory()) {
            log.warn("Templates directory not found: {}", templatesDir.getAbsolutePath());
            return templates;
        }

        File[] files = templatesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return templates;
        }

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                Map<String, Object> template = objectMapper.readValue(content, Map.class);
                templates.add(template);
                log.debug("Loaded template: {}", template.get("template_code"));
            } catch (Exception e) {
                log.error("Failed to load template file: {}", file.getName(), e);
            }
        }

        return templates;
    }
}