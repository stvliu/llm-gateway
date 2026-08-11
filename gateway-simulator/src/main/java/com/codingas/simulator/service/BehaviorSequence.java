/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.simulator.service;

import java.util.List;
import java.util.Optional;

/**
 * 行为序列，按预定义 HTTP 状态码序列返回响应。
 * <p>
 * 支持一次性（消费完恢复全局模式）和循环（loop=true）两种模式。
 * 用于熔断器生命周期验证等场景。
 */
public class BehaviorSequence {

    private final List<Integer> steps;
    private final boolean loop;
    private int currentIndex;
    private boolean active;

    /**
     * 构造行为序列。
     *
     * @param steps HTTP 状态码序列
     * @param loop  是否循环
     */
    public BehaviorSequence(List<Integer> steps, boolean loop) {
        this.steps = List.copyOf(steps);
        this.loop = loop;
        this.currentIndex = 0;
        this.active = true;
    }

    /**
     * 消费当前步进，返回对应的 SimulatorMode。
     * <p>
     * 序列耗尽时：loop=true 重置索引，loop=false 标记 inactive 返回 empty。
     *
     * @return 对应的 SimulatorMode，无活跃序列时返回 empty
     */
    public synchronized Optional<SimulatorModeService.SimulatorMode> consume() {
        if (!active || steps.isEmpty()) {
            return Optional.empty();
        }
        int statusCode = steps.get(currentIndex);
        currentIndex++;
        if (currentIndex >= steps.size()) {
            if (loop) {
                currentIndex = 0;
            } else {
                active = false;
            }
        }
        return Optional.of(httpStatusToMode(statusCode));
    }

    /**
     * 获取当前步进索引。
     *
     * @return 当前步进索引
     */
    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 获取序列总步数。
     *
     * @return 步数
     */
    public int size() {
        return steps.size();
    }

    /**
     * 是否活跃。
     *
     * @return 是否还有未消费的步进
     */
    public synchronized boolean isActive() {
        return active;
    }

    /**
     * 是否循环模式。
     *
     * @return 是否循环
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * 获取步骤副本。
     *
     * @return 不可修改的步骤列表
     */
    public List<Integer> getSteps() {
        return List.copyOf(steps);
    }

    /**
     * 重置序列到初始状态。
     */
    public synchronized void reset() {
        this.currentIndex = 0;
        this.active = true;
    }

    /**
     * HTTP 状态码到 SimulatorMode 的映射。
     *
     * @param statusCode HTTP 状态码
     * @return 对应的 SimulatorMode
     * @throws IllegalArgumentException 不支持的 HTTP 状态码
     */
    private static SimulatorModeService.SimulatorMode httpStatusToMode(int statusCode) {
        return switch (statusCode) {
            case 200 -> SimulatorModeService.SimulatorMode.NORMAL;
            case 401 -> SimulatorModeService.SimulatorMode.AUTH_ERROR;
            case 429 -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
            case 400 -> SimulatorModeService.SimulatorMode.INVALID_REQUEST;
            case 500 -> SimulatorModeService.SimulatorMode.UPSTREAM_ERROR;
            case 503 -> SimulatorModeService.SimulatorMode.SERVICE_DOWN;
            case 408 -> SimulatorModeService.SimulatorMode.TIMEOUT;
            default -> throw new IllegalArgumentException("不支持的状态码: " + statusCode);
        };
    }
}
