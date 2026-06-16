package com.codingas.simulator.service;

/**
 * 延迟配置，控制模拟器响应延迟。
 * <p>
 * 独立于模式的正交配置，可与任何模式组合使用。
 */
public class DelayConfig {

    private long fixedDelayMs;
    private boolean active;

    public DelayConfig() {
        this.fixedDelayMs = 0;
        this.active = false;
    }

    /**
     * 设置固定延迟。
     *
     * @param delayMs 延迟毫秒数
     */
    public synchronized void setDelay(long delayMs) {
        this.fixedDelayMs = delayMs;
        this.active = delayMs > 0;
    }

    /**
     * 清除延迟配置。
     */
    public synchronized void clearDelay() {
        this.fixedDelayMs = 0;
        this.active = false;
    }

    /**
     * 获取固定延迟毫秒数。
     *
     * @return 延迟毫秒数
     */
    public synchronized long getFixedDelayMs() {
        return fixedDelayMs;
    }

    /**
     * 是否启用延迟。
     *
     * @return 是否启用
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * 应用延迟，当前线程 sleep。
     */
    public synchronized void applyDelay() {
        if (active && fixedDelayMs > 0) {
            try {
                Thread.sleep(fixedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
