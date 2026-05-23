package com.codingas.gateway.infrastructure.alert.gateway;

import com.codingas.gateway.domain.dataprotection.entity.SensitiveDataRule;
import com.codingas.gateway.infrastructure.alert.gateway.database.SensitiveDataRuleRepository;
import com.codingas.gateway.infrastructure.alert.gateway.database.dataobject.SensitiveDataRuleDo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SensitiveDataRuleGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SensitiveDataRuleGatewayImpl 测试")
class SensitiveDataRuleGatewayImplTest {

    @Mock
    private SensitiveDataRuleRepository repository;

    @Mock
    private SensitiveDataRuleConverter converter;

    @InjectMocks
    private SensitiveDataRuleGatewayImpl gateway;

    @Nested
    @DisplayName("findByRuleCode 测试")
    class FindByRuleCodeTests {

        @Test
        @DisplayName("根据规则编码查找")
        void findByRuleCode_found() {
            // Given
            SensitiveDataRuleDo ruleDo = createTestRuleDo();
            SensitiveDataRule rule = createTestRule();

            when(repository.findByRuleName("rule-1")).thenReturn(Optional.of(ruleDo));
            when(converter.toDomain(ruleDo)).thenReturn(rule);

            // When
            Optional<SensitiveDataRule> result = gateway.findByRuleCode("rule-1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(rule);
        }

        @Test
        @DisplayName("规则编码不存在")
        void findByRuleCode_notFound() {
            // Given
            when(repository.findByRuleName("unknown")).thenReturn(Optional.empty());

            // When
            Optional<SensitiveDataRule> result = gateway.findByRuleCode("unknown");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEnabledTrue 测试")
    class FindByEnabledTrueTests {

        @Test
        @DisplayName("查找所有启用的规则")
        void findByEnabledTrue() {
            // Given
            List<SensitiveDataRuleDo> ruleDos = List.of(createTestRuleDo());
            List<SensitiveDataRule> rules = List.of(createTestRule());

            when(repository.findByEnabled(true)).thenReturn(ruleDos);
            when(converter.toDomainList(ruleDos)).thenReturn(rules);

            // When
            List<SensitiveDataRule> result = gateway.findByEnabledTrue();

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByDataType 测试")
    class FindByDataTypeTests {

        @Test
        @DisplayName("根据数据类型查找规则")
        void findByDataType() {
            // Given
            List<SensitiveDataRuleDo> ruleDos = List.of(createTestRuleDo());
            List<SensitiveDataRule> rules = List.of(createTestRule());

            when(repository.findByDataType("PHONE")).thenReturn(ruleDos);
            when(converter.toDomainList(ruleDos)).thenReturn(rules);

            // When
            List<SensitiveDataRule> result = gateway.findByDataType("PHONE");

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("existsByRuleCode 测试")
    class ExistsByRuleCodeTests {

        @Test
        @DisplayName("规则编码存在返回 true")
        void existsByRuleCode_exists_returnsTrue() {
            // Given
            when(repository.existsByRuleName("rule-1")).thenReturn(true);

            // When
            boolean result = gateway.existsByRuleCode("rule-1");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("规则编码不存在返回 false")
        void existsByRuleCode_notExists_returnsFalse() {
            // Given
            when(repository.existsByRuleName("unknown")).thenReturn(false);

            // When
            boolean result = gateway.existsByRuleCode("unknown");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("count 测试")
    class CountTests {

        @Test
        @DisplayName("统计规则数量")
        void count() {
            // Given
            when(repository.count()).thenReturn(5L);

            // When
            long result = gateway.count();

            // Then
            assertThat(result).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("save 测试")
    class SaveTests {

        @Test
        @DisplayName("保存单个规则")
        void save() {
            // Given
            SensitiveDataRule rule = createTestRule();
            SensitiveDataRuleDo ruleDo = createTestRuleDo();

            when(converter.toDataObject(rule)).thenReturn(ruleDo);
            when(repository.save(ruleDo)).thenReturn(ruleDo);
            when(converter.toDomain(ruleDo)).thenReturn(rule);

            // When
            SensitiveDataRule result = gateway.save(rule);

            // Then
            assertThat(result).isEqualTo(rule);
            verify(repository).save(ruleDo);
        }
    }

    @Nested
    @DisplayName("saveAll 测试")
    class SaveAllTests {

        @Test
        @DisplayName("批量保存规则")
        void saveAll() {
            // Given
            List<SensitiveDataRule> rules = List.of(createTestRule());
            List<SensitiveDataRuleDo> ruleDos = List.of(createTestRuleDo());

            when(converter.toDataObjectList(rules)).thenReturn(ruleDos);
            when(repository.save(any())).thenReturn(createTestRuleDo());
            when(converter.toDomainList(any())).thenReturn(rules);

            // When
            List<SensitiveDataRule> result = gateway.saveAll(rules);

            // Then
            assertThat(result).hasSize(1);
            verify(repository).save(any());
        }
    }

    // Helper methods
    private SensitiveDataRuleDo createTestRuleDo() {
        SensitiveDataRuleDo ruleDo = new SensitiveDataRuleDo();
        ruleDo.setId(1L);
        ruleDo.setRuleName("rule-1");
        ruleDo.setDataType("PHONE");
        ruleDo.setEnabled(true);
        return ruleDo;
    }

    private SensitiveDataRule createTestRule() {
        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setRuleName("rule-1");
        rule.setDataType("PHONE");
        rule.setEnabled(true);
        return rule;
    }
}
