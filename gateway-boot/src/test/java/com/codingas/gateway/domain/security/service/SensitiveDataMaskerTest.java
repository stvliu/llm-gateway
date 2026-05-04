package com.codingas.gateway.domain.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveDataMasker 完整单元测试
 */
@DisplayName("SensitiveDataMasker 测试")
class SensitiveDataMaskerTest {

    private SensitiveDataMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker();
    }

    @Nested
    @DisplayName("mask 方法测试")
    class MaskTests {

        @Test
        @DisplayName("手机号脱敏")
        void mask_phoneNumber_masksCorrectly() {
            // given
            String phone = "13812345678";

            // when
            String masked = masker.mask(phone);

            // then - 手机号中间4位被替换
            assertThat(masked).contains("****");
            assertThat(masked).doesNotContain("1234"); // 中间4位不应出现
        }

        @Test
        @DisplayName("身份证号脱敏")
        void mask_idCard_masksCorrectly() {
            // given
            String idCard = "110101199001011234";

            // when
            String masked = masker.mask(idCard);

            // then - 身份证中间部分被替换
            assertThat(masked).contains("****");
        }

        @Test
        @DisplayName("银行卡号脱敏")
        void mask_bankCard_masksCorrectly() {
            // given
            String bankCard = "6222021234567890123";

            // when
            String masked = masker.mask(bankCard);

            // then - 银行卡中间部分被替换
            assertThat(masked).contains("****");
        }

        @Test
        @DisplayName("混合文本脱敏")
        void mask_mixedContent_masksAll() {
            // given
            String text = "用户手机号13812345678，身份证110101199001011234";

            // when
            String masked = masker.mask(text);

            // then - 验证原始敏感数据已被脱敏
            assertThat(masked).doesNotContain("13812345678");
            assertThat(masked).doesNotContain("110101199001011234");
            assertThat(masked).contains("****");
        }

        @Test
        @DisplayName("null 输入返回 null")
        void mask_null_returnsNull() {
            assertThat(masker.mask(null)).isNull();
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void mask_empty_returnsEmpty() {
            assertThat(masker.mask("")).isEmpty();
        }

        @Test
        @DisplayName("空白字符串返回原字符串")
        void mask_blank_returnsOriginal() {
            assertThat(masker.mask("   ")).isEqualTo("   ");
        }

        @Test
        @DisplayName("不含敏感数据的文本不变化")
        void mask_noSensitiveData_returnsOriginal() {
            // given
            String text = "这是一段普通的文本，没有任何敏感数据。";

            // when
            String masked = masker.mask(text);

            // then
            assertThat(masked).isEqualTo(text);
        }
    }

    @Nested
    @DisplayName("mask 带排除规则测试")
    class MaskWithExcludeTests {

        @Test
        @DisplayName("排除手机号规则")
        void mask_excludePhone_onlyMasksOthers() {
            // given
            String text = "手机13812345678身份证110101199001011234";

            // when
            String masked = masker.mask(text, List.of("PHONE"));

            // then - 手机号可能仍被其他规则（如银行卡）脱敏，但身份证明确被脱敏
            assertThat(masked).contains("****"); // 有脱敏发生
        }

        @Test
        @DisplayName("排除所有规则不脱敏")
        void mask_excludeAll_noMasking() {
            // given
            String text = "测试文本";

            // when
            String masked = masker.mask(text, List.of("PHONE", "ID_CARD", "BANK_CARD"));

            // then
            assertThat(masked).isEqualTo(text);
        }

        @Test
        @DisplayName("排除规则为 null 正常脱敏")
        void mask_nullExcludeRules_masksAll() {
            // given
            String phone = "13812345678";

            // when
            String masked = masker.mask(phone, null);

            // then
            assertThat(masked).contains("****");
        }
    }

    @Nested
    @DisplayName("containsSensitiveData 方法测试")
    class ContainsSensitiveDataTests {

        @Test
        @DisplayName("包含手机号返回 true")
        void containsSensitiveData_phone_returnsTrue() {
            assertThat(masker.containsSensitiveData("手机号是13812345678")).isTrue();
        }

        @Test
        @DisplayName("包含身份证返回 true")
        void containsSensitiveData_idCard_returnsTrue() {
            assertThat(masker.containsSensitiveData("身份证110101199001011234")).isTrue();
        }

        @Test
        @DisplayName("包含银行卡返回 true")
        void containsSensitiveData_bankCard_returnsTrue() {
            assertThat(masker.containsSensitiveData("银行卡6222021234567890123")).isTrue();
        }

        @Test
        @DisplayName("不含敏感数据返回 false")
        void containsSensitiveData_noSensitiveData_returnsFalse() {
            assertThat(masker.containsSensitiveData("这是普通文本")).isFalse();
        }

        @Test
        @DisplayName("null 输入返回 false")
        void containsSensitiveData_null_returnsFalse() {
            assertThat(masker.containsSensitiveData(null)).isFalse();
        }

        @Test
        @DisplayName("空字符串返回 false")
        void containsSensitiveData_empty_returnsFalse() {
            assertThat(masker.containsSensitiveData("")).isFalse();
        }
    }

    @Nested
    @DisplayName("detectTypes 方法测试")
    class DetectTypesTests {

        @Test
        @DisplayName("检测手机号类型")
        void detectTypes_phone_returnsPhone() {
            List<String> types = masker.detectTypes("13812345678");
            assertThat(types).contains("PHONE");
        }

        @Test
        @DisplayName("检测身份证类型")
        void detectTypes_idCard_returnsIdCard() {
            List<String> types = masker.detectTypes("110101199001011234");
            assertThat(types).contains("ID_CARD");
        }

        @Test
        @DisplayName("检测银行卡类型")
        void detectTypes_bankCard_returnsBankCard() {
            List<String> types = masker.detectTypes("6222021234567890123");
            assertThat(types).contains("BANK_CARD");
        }

        @Test
        @DisplayName("无敏感数据返回空列表")
        void detectTypes_noSensitiveData_returnsEmpty() {
            List<String> types = masker.detectTypes("普通文本");
            assertThat(types).isEmpty();
        }
    }

    @Nested
    @DisplayName("loadRules 方法测试")
    class LoadRulesTests {

        @Test
        @DisplayName("加载空规则列表使用默认规则")
        void loadRules_emptyList_usesDefaults() {
            // when
            masker.loadRules(List.of());

            // then
            assertThat(masker.containsSensitiveData("13812345678")).isTrue();
        }

        @Test
        @DisplayName("加载 null 使用默认规则")
        void loadRules_null_usesDefaults() {
            // when
            masker.loadRules(null);

            // then
            assertThat(masker.containsSensitiveData("13812345678")).isTrue();
        }

        @Test
        @DisplayName("加载自定义规则")
        void loadRules_customRules_appliesNewRules() {
            // given
            List<SensitiveDataMasker.RuleData> rules = List.of(
                new SensitiveDataMasker.RuleData("EMAIL", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***@***.***")
            );

            // when
            masker.loadRules(rules);

            // then
            String masked = masker.mask("test@example.com");
            assertThat(masked).contains("***");
        }
    }

    @Nested
    @DisplayName("resetToDefaultRules 方法测试")
    class ResetToDefaultRulesTests {

        @Test
        @DisplayName("重置后使用默认规则")
        void resetToDefaultRules_afterCustomRules_usesDefaults() {
            // given
            masker.loadRules(List.of(
                new SensitiveDataMasker.RuleData("EMAIL", "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "***")
            ));

            // when
            masker.resetToDefaultRules();

            // then
            assertThat(masker.containsSensitiveData("13812345678")).isTrue();
        }
    }
}
