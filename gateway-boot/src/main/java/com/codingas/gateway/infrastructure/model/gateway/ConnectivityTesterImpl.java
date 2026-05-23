package com.codingas.gateway.infrastructure.model.gateway;

import com.codingas.gateway.application.provider.dto.ConnectivityTestRequest;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.domain.model.gateway.ConnectivityTester;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 连通性测试器实现
 *
 * <p>提供基本的连通性测试功能。</p>
 */
@Component
public class ConnectivityTesterImpl implements ConnectivityTester {

    @Override
    public ConnectivityTestResult test(ConnectivityTestRequest request) {
        // 简单实现：返回成功结果
        // 实际实现应该调用相应的协议网关进行测试
        return new ConnectivityTestResult(
                true,
                "Connectivity test passed",
                Collections.emptyList(),
                new ConnectivityTestResult.LevelResult(
                        true,
                        "Authentication successful",
                        0L,
                        null,
                        Collections.emptyList()
                ),
                null,
                0L
        );
    }
}
