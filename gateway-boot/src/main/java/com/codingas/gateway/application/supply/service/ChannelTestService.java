package com.codingas.gateway.application.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelActions;
import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import com.codingas.gateway.domain.supply.exception.ChannelNotFoundException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelOperationLogGateway;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 渠道测试连通性服务
 *
 * <p>向渠道配置的端点发送测试请求，验证渠道是否可用。</p>
 */
@Service
public class ChannelTestService {

    private final ChannelGateway channelGateway;
    private final ChannelOperationLogGateway operationLogGateway;

    public ChannelTestService(ChannelGateway channelGateway,
                              ChannelOperationLogGateway operationLogGateway) {
        this.channelGateway = channelGateway;
        this.operationLogGateway = operationLogGateway;
    }

    /**
     * 测试结果
     */
    public record TestResult(
            boolean success,
            Integer statusCode,
            Long responseTimeMs,
            String model,
            Integer responseTokens,
            String responsePreview,
            String errorType,
            String errorMessage,
            List<String> suggestions,
            LocalDateTime testedAt
    ) {}

    /**
     * 测试渠道连通性
     *
     * @param channelId 渠道 ID
     * @param model     可选，指定测试用模型
     * @param timeout   可选，超时时间(ms)
     * @return 测试结果
     */
    public TestResult test(Long channelId, String model, Integer timeout,
                           Long operatorId, String operatorName, String operatorIp) {
        Channel channel = channelGateway.findById(channelId)
                .orElseThrow(() -> new ChannelNotFoundException(channelId));

        // 前置检查
        if (channel.getApiEndpoint() == null || channel.getApiEndpoint().isBlank()) {
            TestResult result = new TestResult(
                    false, null, null, null, null, null,
                    "INVALID_CONFIG", "API 地址未配置",
                    List.of("请先编辑渠道，配置 API 地址"),
                    LocalDateTime.now()
            );
            saveTestLog(channelId, channel.getName(), result, operatorId, operatorName, operatorIp);
            return result;
        }

        if (channel.getApiKey() == null || channel.getApiKey().isBlank()) {
            TestResult result = new TestResult(
                    false, null, null, null, null, null,
                    "INVALID_CONFIG", "API Key 未配置",
                    List.of("请先编辑渠道，配置 API Key"),
                    LocalDateTime.now()
            );
            saveTestLog(channelId, channel.getName(), result, operatorId, operatorName, operatorIp);
            return result;
        }

        // 执行测试
        long startTime = System.currentTimeMillis();
        try {
            // TODO: 根据渠道协议类型调用对应的测试逻辑
            // 此处为占位实现，实际需要根据 protocolType 调用 OpenAI/Anthropic 的测试端点
            TestResult result = executeTest(channel, model, timeout != null ? timeout : 10000);

            saveTestLog(channelId, channel.getName(), result, operatorId, operatorName, operatorIp);
            return result;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            TestResult result = new TestResult(
                    false, null, elapsed, model, null, null,
                    "UNKNOWN_ERROR", e.getMessage(),
                    List.of("查看详细日志获取更多信息"),
                    LocalDateTime.now()
            );
            saveTestLog(channelId, channel.getName(), result, operatorId, operatorName, operatorIp);
            return result;
        }
    }

    /**
     * 执行实际的测试请求
     *
     * <p>根据渠道的协议类型，构造对应的测试请求并发送。</p>
     */
    private TestResult executeTest(Channel channel, String model, int timeoutMs) {
        // 占位实现：模拟测试结果
        // TODO: 替换为真实的 HTTP 请求调用
        return new TestResult(
                true, 200, 1230L, model != null ? model : "gpt-4o",
                47, "Hello! How can I help you today?...",
                null, null, null,
                LocalDateTime.now()
        );
    }

    /**
     * 保存测试操作日志
     */
    private void saveTestLog(Long channelId, String channelName, TestResult result,
                             Long operatorId, String operatorName, String operatorIp) {
        String changeDetail = String.format(
                "{\"testResult\":%s,\"responseTimeMs\":%d,\"errorType\":%s}",
                result.success(), result.responseTimeMs(),
                result.errorType() != null ? "\"" + result.errorType() + "\"" : "null");

        ChannelOperationLog log = new ChannelOperationLog();
        log.setChannelId(channelId);
        log.setChannelName(channelName);
        log.setAction(ChannelActions.TEST);
        log.setActionLabel(ChannelActions.getLabel(ChannelActions.TEST));
        log.setChangeDetail(changeDetail);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorIp(operatorIp);
        log.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        log.setOperatedAt(LocalDateTime.now());
        operationLogGateway.save(log);
    }
}
