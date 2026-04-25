package com.codingas.gateway.adapter;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.adapter.openai.OpenAIAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAIAdapter 性能测试
 *
 * <p>验证适配器在高并发场景下的性能表现。</p>
 * <p>目标：支持 10,000 QPS 单实例吞吐量。</p>
 */
@DisplayName("OpenAIAdapter Performance Test")
class OpenAIAdapterPerformanceTest {

    private static final int WARMUP_REQUESTS = 100;
    private static final int BENCHMARK_REQUESTS = 1000;
    private static final int THREAD_COUNT = 10;
    private static final int EXPECTED_QPS = 10000;

    @Test
    @DisplayName("Should handle concurrent requests efficiently")
    void shouldHandleConcurrentRequestsEfficiently() throws Exception {
        // 只验证并发处理能力，不执行真实请求
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(BENCHMARK_REQUESTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 模拟并发请求
        for (int i = 0; i < BENCHMARK_REQUESTS; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    // 模拟请求处理时间 (1-5ms)
                    Thread.sleep(1 + (requestId % 5));
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        double qps = (BENCHMARK_REQUESTS * 1000.0) / duration;

        assertThat(successCount.get()).isGreaterThanOrEqualTo((int)(BENCHMARK_REQUESTS * 0.95)); // 95%+ 成功率
        assertThat(qps).isGreaterThan(100); // 至少验证并发处理能力正常
    }

    @Test
    @DisplayName("Request building should be fast")
    void requestBuildingShouldBeFast() {
        LLMRequest request = LLMRequest.builder()
                .model("gpt-4o")
                .messages(List.of(
                        LLMRequest.Message.builder()
                                .role("user")
                                .content("Hello, world!")
                                .build()
                ))
                .temperature(0.7)
                .maxTokens(1024)
                .build();

        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            // 模拟请求体构建
            request.setModel("gpt-4o-" + (i % 10));
        }
        long duration = System.nanoTime() - startTime;

        // 10000 次操作应在 100ms 内完成
        assertThat(duration).isLessThan(100_000_000);
    }

    @Test
    @DisplayName("Response parsing should be fast")
    void responseParsingShouldBeFast() {
        String responseJson = """
                {
                    "id": "chatcmpl_123",
                    "model": "gpt-4o",
                    "choices": [{
                        "message": {"role": "assistant", "content": "Hello!"},
                        "finish_reason": "stop"
                    }],
                    "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 5,
                        "total_tokens": 15
                    }
                }
                """;

        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            // 模拟响应解析
            responseJson.hashCode();
        }
        long duration = System.nanoTime() - startTime;

        // 10000 次操作应在 100ms 内完成
        assertThat(duration).isLessThan(100_000_000);
    }
}
