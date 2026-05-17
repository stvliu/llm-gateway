package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.application.provider.dto.ConnectivityTestRequest;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.domain.model.enums.ProviderType;
import com.codingas.gateway.domain.model.gateway.ConnectivityTester;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterBuilderFactory;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连通性测试器实现
 *
 * <p>委托给各供应商的 Adapter 执行连通性测试。</p>
 * <p>Adapter 负责具体的 API 调用和测试逻辑。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectivityTesterImpl implements ConnectivityTester {

    private final AdapterBuilderFactory adapterBuilderFactory;

    @Override
    public ConnectivityTestResult test(ConnectivityTestRequest request) {
        ProviderType providerType = request.providerType();
        String apiKey = request.apiKey();

        log.info("Starting connectivity test for provider: {}", providerType);

        // 创建临时 Adapter（使用请求中的 baseUrl 或默认值）
        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            providerType,
            request.baseUrl(),
            apiKey
        );

        // 委托给 Adapter 执行测试
        ConnectivityTestResult result = adapter.testConnectivity(
            apiKey,
            request.baseUrl(),
            request.model()
        );

        log.info("Connectivity test completed for provider {}: success={}, latency={}ms",
            providerType, result.success(), result.totalLatencyMs());

        return result;
    }
}
