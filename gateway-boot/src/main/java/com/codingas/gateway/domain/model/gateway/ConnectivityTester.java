package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.application.provider.dto.ConnectivityTestRequest;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;

/**
 * 连通性测试器 Gateway 接口
 *
 * <p>负责执行分层连通性测试：</p>
 * <ul>
 *   <li>Level 1：认证验证（获取模型列表或最小请求）</li>
 *   <li>Level 2：模型可用性验证（发送最小 chat 请求）</li>
 * </ul>
 */
public interface ConnectivityTester {

    /**
     * 执行分层连通性测试
     *
     * @param request 测试请求
     * @return 测试结果
     */
    ConnectivityTestResult test(ConnectivityTestRequest request);
}
