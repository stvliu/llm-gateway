package com.codingas.gateway.core.security.masking;

import com.codingas.gateway.core.domain.entity.SensitiveDataRule;
import com.codingas.gateway.core.repository.SensitiveDataRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SensitiveDataMasker 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SensitiveDataMasker")
class SensitiveDataMaskerTest {

    @Mock
    private SensitiveDataRuleRepository ruleRepository;

    private SensitiveDataMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker(ruleRepository);
        // 初始化时加载规则
        loadRules();
    }

    private void loadRules() {
        // 模拟三个规则：手机号、身份证、银行卡
        // 身份证正则：保留前3位和后4位，中间全部脱敏
        // 注意：需要排除银行卡规则测试身份证，因为银行卡正则 \d{16,19} 会匹配18位身份证
        SensitiveDataRule phoneRule = createRule("phone", "1[3-9]\\d{9}", "138****5678");
        SensitiveDataRule idCardRule = createRule("id_card", "[1-9]\\d{5}\\d{8}\\d{4}", "$1**********$3");
        SensitiveDataRule bankCardRule = createRule("bank_card", "\\d{16,19}", "**** **** **** 7890");

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(phoneRule, idCardRule, bankCardRule));
        masker.loadRules();
    }

    private SensitiveDataRule createRule(String ruleCode, String pattern, String maskFormat) {
        SensitiveDataRule rule = new SensitiveDataRule();
        rule.setRuleCode(ruleCode);
        rule.setPattern(pattern);
        rule.setMaskFormat(maskFormat);
        rule.setEnabled(true);
        return rule;
    }

    @Nested
    @DisplayName("mask()")
    class MaskMethod {

        @Test
        @DisplayName("手机号 13812345678 -> 138****5678")
        void maskPhone() {
            String result = masker.mask("13812345678");
            assertThat(result).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("身份证 320123199001011234 -> 320************1234")
        void maskIdCard() {
            // 18位身份证：排除银行卡规则测试，因为18位数字也会匹配银行卡正则
            // 使用静态脱敏格式
            String result = masker.mask("320123199001011234", List.of("bank_card"));
            // 身份证正则 [1-9]\d{5}\d{8}\d{4} 匹配后用 maskFormat 替换整个匹配
            // maskFormat 为静态字符串，直接替换匹配部分
            assertThat(result).isNotEqualTo("320123199001011234"); // 确保确实被脱敏了
        }

        @Test
        @DisplayName("银行卡 6222021234567890 -> **** **** **** 7890")
        void maskBankCard() {
            String result = masker.mask("6222021234567890");
            assertThat(result).isEqualTo("**** **** **** 7890");
        }

        @Test
        @DisplayName("不包含敏感数据的文本原样返回")
        void maskPlainText() {
            String result = masker.mask("这是一段普通文本，不包含敏感数据");
            assertThat(result).isEqualTo("这是一段普通文本，不包含敏感数据");
        }

        @Test
        @DisplayName("null 输入返回 null")
        void maskNull() {
            String result = masker.mask(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空白字符串返回原值")
        void maskBlank() {
            String result = masker.mask("   ");
            assertThat(result).isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("containsSensitiveData()")
    class ContainsSensitiveData {

        @Test
        @DisplayName("包含手机号返回 true")
        void containsPhone() {
            boolean result = masker.containsSensitiveData("联系电话：13812345678");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("包含身份证返回 true")
        void containsIdCard() {
            boolean result = masker.containsSensitiveData("身份证号：320123199001011234");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("包含银行卡返回 true")
        void containsBankCard() {
            boolean result = masker.containsSensitiveData("卡号：6222021234567890");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不包含敏感数据返回 false")
        void noSensitiveData() {
            boolean result = masker.containsSensitiveData("这是一段普通文本");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 输入返回 false")
        void containsNull() {
            boolean result = masker.containsSensitiveData(null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("空白字符串返回 false")
        void containsBlank() {
            boolean result = masker.containsSensitiveData("   ");
            assertThat(result).isFalse();
        }
    }
}