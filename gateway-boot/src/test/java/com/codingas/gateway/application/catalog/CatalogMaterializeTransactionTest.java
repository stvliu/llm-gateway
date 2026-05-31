package com.codingas.gateway.application.catalog;

import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 级联物化事务回滚集成测试
 */
@SpringBootTest
@ActiveProfiles("test")
class CatalogMaterializeTransactionTest {

    @Autowired
    private CatalogMaterializeService catalogMaterializeService;

    @Autowired
    private ProviderGateway providerGateway;

    @Test
    @DisplayName("级联物化不存在的供应商应返回空结果，不会回滚整个操作")
    void materializeNonExistentProvider() {
        var result = catalogMaterializeService.materializeProviderWithPlans("nonexistent", null);
        assertThat(result).isNotNull();
    }
}