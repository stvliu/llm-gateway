package com.codingas.gateway.web.advice;

import com.codingas.gateway.core.security.masking.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {MaskingResponseAdviceTest.TestConfig.class, MaskingTestController.class})
@DisplayName("MaskingResponseAdvice Tests")
class MaskingResponseAdviceTest {

    @Configuration
    static class TestConfig {
        private final SensitiveDataMasker sensitiveDataMasker;

        TestConfig() {
            this.sensitiveDataMasker = mock(SensitiveDataMasker.class);
            when(sensitiveDataMasker.mask(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            when(sensitiveDataMasker.mask("13812345678")).thenReturn("138****5678");
        }

        @Bean
        public SensitiveDataMasker sensitiveDataMasker() {
            return sensitiveDataMasker;
        }

        @Bean
        public MaskingResponseAdvice maskingResponseAdvice() {
            return new MaskingResponseAdvice(sensitiveDataMasker);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SensitiveDataMasker sensitiveDataMasker;

    @Nested
    @DisplayName("mask(String)")
    class MaskStringTests {

        @Test
        @DisplayName("对字符串响应进行脱敏")
        void maskStringResponse() throws Exception {
            mockMvc.perform(get("/mask-test/string")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("138****5678"));
        }

        @Test
        @DisplayName("空字符串不处理")
        void emptyString_returnsOriginal() throws Exception {
            mockMvc.perform(get("/mask-test/empty")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }
    }

    @Nested
    @DisplayName("不需要脱敏的场景")
    class NoMaskingTests {

        @Test
        @DisplayName("Map 类型响应不进行字符串脱敏")
        void mapResponse_notMasked() throws Exception {
            mockMvc.perform(get("/mask-test/map")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("test"));
        }
    }
}
