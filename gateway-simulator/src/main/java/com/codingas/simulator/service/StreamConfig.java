package com.codingas.simulator.service;

/**
 * 流控制配置，控制 SSE 流式响应的行为。
 * <p>
 * 支持多种流行为：正常、中断、非法数据、重复结束标记、空 SSE、不完整流。
 */
public class StreamConfig {

    private String action = "normal";
    private int chunkCount = 3;
    private int chunkIntervalMs = 50;
    private int interruptAfter = 0;
    private String invalidChunk = "";

    /**
     * 配置流行为。
     *
     * @param action          流行为类型
     * @param chunkCount      chunk 数量
     * @param chunkIntervalMs chunk 间隔毫秒
     * @param interruptAfter  中断前发送的 chunk 数
     * @param invalidChunk    非法数据内容
     */
    public void configure(String action, int chunkCount, int chunkIntervalMs,
                          int interruptAfter, String invalidChunk) {
        this.action = action != null ? action : "normal";
        this.chunkCount = chunkCount > 0 ? chunkCount : 3;
        this.chunkIntervalMs = chunkIntervalMs > 0 ? chunkIntervalMs : 50;
        this.interruptAfter = interruptAfter;
        this.invalidChunk = invalidChunk != null ? invalidChunk : "";
    }

    /**
     * 重置为默认配置。
     */
    public void reset() {
        this.action = "normal";
        this.chunkCount = 3;
        this.chunkIntervalMs = 50;
        this.interruptAfter = 0;
        this.invalidChunk = "";
    }

    public String getAction() { return action; }
    public int getChunkCount() { return chunkCount; }
    public int getChunkIntervalMs() { return chunkIntervalMs; }
    public int getInterruptAfter() { return interruptAfter; }
    public String getInvalidChunk() { return invalidChunk; }
}
