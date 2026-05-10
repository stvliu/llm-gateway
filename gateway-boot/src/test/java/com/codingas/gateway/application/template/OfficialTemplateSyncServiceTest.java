package com.codingas.gateway.application.template;

import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.gateway.ProviderTemplateGateway;
import com.codingas.gateway.infrastructure.template.repository.BuiltinTemplateLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@SpringBootTest
class OfficialTemplateSyncServiceTest {

    @Autowired
    private BuiltinTemplateLoader builtinTemplateLoader;

    @Autowired
    private ProviderTemplateGateway gateway;

    @Autowired
    private OfficialTemplateSyncService syncService;

    @Test
    void testLoadBuiltinTemplates() {
        List<Map<String, Object>> templates = builtinTemplateLoader.loadBuiltinTemplates();
        System.out.println("Loaded " + templates.size() + " templates");
        for (Map<String, Object> t : templates) {
            System.out.println("  - " + t.get("template_code") + ": " + t.get("template_name"));
        }
    }

    @Test
    void testSyncTemplates() {
        try {
            var result = syncService.syncBuiltinTemplates();
            System.out.println("Sync result: " + result);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testFindAll() {
        List<ProviderTemplate> templates = gateway.findOfficialTemplates();
        System.out.println("Found " + templates.size() + " official templates");
        for (ProviderTemplate t : templates) {
            System.out.println("  - " + t.getTemplateCode() + ": " + t.getTemplateName() + " (status=" + t.getState() + ")");
        }
    }
}
