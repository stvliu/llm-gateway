package com.codingas.simulator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 模拟模式管理服务，管理模拟器运行模式和请求记录。
 * <p>
 * 支持三种模式：NORMAL（正常响应）、RATE_LIMITED（限流响应）、FAULT（故障响应）。
 * 请求记录使用环形缓冲区，超出容量时自动丢弃最旧记录。
 */
@Component
public class SimulatorModeService {

    /**
     * 模拟器运行模式枚举。
     */
    public enum SimulatorMode {
        /** 正常模式，返回成功响应 */
        NORMAL,
        /** 认证错误，返回 401 */
        AUTH_ERROR,
        /** 限流模式，返回 429 rate_limit_error */
        RATE_LIMITED,
        /** 配额超限，返回 429 insufficient_quota */
        QUOTA_EXCEEDED,
        /** 非法请求，返回 400 */
        INVALID_REQUEST,
        /** 上游错误，返回 500 */
        UPSTREAM_ERROR,
        /** 服务不可用，返回 503 */
        SERVICE_DOWN,
        /** 超时，返回 408 */
        TIMEOUT,
        /** 行为序列模式，委托给 BehaviorSequence */
        INTERMITTENT
    }

    /** 当前模式，volatile 保证线程可见性 */
    private volatile SimulatorMode mode;

    /** 请求记录环形缓冲区 */
    private final List<RequestRecord> requestLog;

    /** 环形缓冲区容量 */
    private final int logCapacity;

    /** 下一条记录的写入位置（环形指针） */
    private int writeIndex;

    /** 行为序列（可选，非空时优先于全局模式） */
    private BehaviorSequence behaviorSequence;

    /** 延迟配置 */
    private final DelayConfig delayConfig = new DelayConfig();

    /** 流控制配置 */
    private final StreamConfig streamConfig = new StreamConfig();

    /** API Key 覆盖配置 */
    private final ApiKeyOverrideConfig apiKeyOverrideConfig = new ApiKeyOverrideConfig();

    /**
     * Spring 构造方法，通过 @Value 注入配置初始化模式和缓冲区容量。
     *
     * @param modeConfig  模式配置字符串（normal / rate_limited / fault），默认 normal
     * @param logCapacity 请求记录缓冲区容量，默认 100
     */
    public SimulatorModeService(
            @Value("${simulator.mode:normal}") String modeConfig,
            @Value("${simulator.request-log-capacity:100}") int logCapacity) {
        this.mode = parseMode(modeConfig);
        this.logCapacity = logCapacity;
        this.requestLog = new ArrayList<>(logCapacity);
        this.writeIndex = 0;
    }

    /**
     * 获取当前模拟模式。
     *
     * @return 当前模式
     */
    public SimulatorMode getMode() {
        return mode;
    }

    /**
     * 设置模拟模式。
     *
     * @param mode 目标模式
     */
    public void setMode(SimulatorMode mode) {
        this.mode = mode;
    }

    /**
     * 记录一次请求。
     *
     * @param method HTTP 方法
     * @param path   请求路径
     */
    public synchronized void recordRequest(String method, String path) {
        RequestRecord record = new RequestRecord(method, path, Instant.now());

        if (requestLog.size() < logCapacity) {
            // 缓冲区未满，直接追加
            requestLog.add(record);
        } else {
            // 缓冲区已满，环形覆盖最旧记录
            requestLog.set(writeIndex, record);
        }
        writeIndex = (writeIndex + 1) % logCapacity;
    }

    /**
     * 获取请求记录列表。
     * <p>
     * 返回的列表按时间顺序排列（最旧在前），且不可修改。
     *
     * @return 不可修改的请求记录列表
     */
    public synchronized List<RequestRecord> getRequestLog() {
        if (requestLog.size() < logCapacity) {
            // 缓冲区未满，直接返回
            return Collections.unmodifiableList(new ArrayList<>(requestLog));
        }

        // 缓冲区已满，按写入顺序排列（writeIndex 位置是最旧的）
        List<RequestRecord> ordered = new ArrayList<>(logCapacity);
        for (int i = 0; i < logCapacity; i++) {
            int idx = (writeIndex + i) % logCapacity;
            ordered.add(requestLog.get(idx));
        }
        return Collections.unmodifiableList(ordered);
    }

    /**
     * 将配置字符串解析为 SimulatorMode 枚举。
     *
     * @param modeConfig 模式配置字符串
     * @return 对应的 SimulatorMode 枚举值
     */
    private SimulatorMode parseMode(String modeConfig) {
        return switch (modeConfig.toLowerCase()) {
            case "rate_limited" -> SimulatorMode.RATE_LIMITED;
            case "fault", "upstream_error" -> SimulatorMode.UPSTREAM_ERROR;
            case "auth_error" -> SimulatorMode.AUTH_ERROR;
            case "quota_exceeded" -> SimulatorMode.QUOTA_EXCEEDED;
            case "invalid_request" -> SimulatorMode.INVALID_REQUEST;
            case "service_down" -> SimulatorMode.SERVICE_DOWN;
            case "timeout" -> SimulatorMode.TIMEOUT;
            case "intermittent" -> SimulatorMode.INTERMITTENT;
            default -> SimulatorMode.NORMAL;
        };
    }

    // ==================== BehaviorSequence 方法 ====================

    /**
     * 设置行为序列。
     *
     * @param steps HTTP 状态码序列
     * @param loop  是否循环
     */
    public synchronized void setBehaviorSequence(List<Integer> steps, boolean loop) {
        this.behaviorSequence = new BehaviorSequence(steps, loop);
    }

    /**
     * 获取当前行为序列。
     *
     * @return 行为序列，可能为 null
     */
    public synchronized BehaviorSequence getBehaviorSequence() {
        return behaviorSequence;
    }

    /**
     * 清除行为序列。
     */
    public synchronized void clearBehaviorSequence() {
        this.behaviorSequence = null;
    }

    // ==================== 配置类 getters ====================

    /**
     * 获取延迟配置。
     *
     * @return 延迟配置
     */
    public DelayConfig getDelayConfig() {
        return delayConfig;
    }

    /**
     * 获取流控制配置。
     *
     * @return 流控制配置
     */
    public StreamConfig getStreamConfig() {
        return streamConfig;
    }

    /**
     * 获取 API Key 覆盖配置。
     *
     * @return API Key 覆盖配置
     */
    public ApiKeyOverrideConfig getApiKeyOverrideConfig() {
        return apiKeyOverrideConfig;
    }
}
