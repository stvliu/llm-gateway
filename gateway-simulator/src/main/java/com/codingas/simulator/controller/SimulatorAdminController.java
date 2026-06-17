package com.codingas.simulator.controller;

import com.codingas.simulator.service.BehaviorSequence;
import com.codingas.simulator.service.RequestRecord;
import com.codingas.simulator.service.SimulatorModeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模拟器管理 API Controller，提供模式切换和请求记录查询功能。
 * <p>
 * 端点前缀为 /simulator，支持以下操作：
 * <ul>
 *   <li>POST /simulator/mode — 切换模拟模式</li>
 *   <li>GET /simulator/mode — 获取当前模式</li>
 *   <li>GET /simulator/requests — 查询请求记录</li>
 *   <li>POST /simulator/behavior — 设置行为序列</li>
 *   <li>GET /simulator/behavior — 获取当前行为序列</li>
 *   <li>DELETE /simulator/behavior — 清除行为序列</li>
 *   <li>POST /simulator/delay — 设置延迟</li>
 *   <li>DELETE /simulator/delay — 清除延迟</li>
 *   <li>GET /simulator/delay — 获取当前延迟</li>
 *   <li>POST /simulator/stream — 配置流行为</li>
 *   <li>DELETE /simulator/stream — 重置流配置</li>
 *   <li>GET /simulator/stream — 获取当前流配置</li>
 *   <li>POST /simulator/apikey-override — 设置 API Key 覆盖</li>
 *   <li>DELETE /simulator/apikey-override — 移除 API Key 覆盖</li>
 *   <li>GET /simulator/apikey-override — 获取所有覆盖规则</li>
 * </ul>
 */
@RestController
@RequestMapping("/simulator")
public class SimulatorAdminController {

    private final SimulatorModeService modeService;

    public SimulatorAdminController(SimulatorModeService modeService) {
        this.modeService = modeService;
    }

    /**
     * 获取当前模拟模式。
     *
     * @return 包含当前模式的 Map
     */
    @GetMapping("/mode")
    public ResponseEntity<Map<String, String>> getMode() {
        return ResponseEntity.ok(Map.of("mode", modeService.getMode().name()));
    }

    /**
     * 切换模拟模式。
     *
     * @param body 包含 mode 字段的请求体，值为 normal / rate_limited / fault
     * @return 切换后的模式；如果模式无效返回 400
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@RequestBody Map<String, String> body) {
        String modeStr = body.get("mode");
        if (modeStr == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "缺少 mode 参数"));
        }

        try {
            SimulatorModeService.SimulatorMode mode = parseMode(modeStr);
            modeService.setMode(mode);
            return ResponseEntity.ok(Map.of("mode", mode.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查询请求记录列表。
     *
     * @return 请求记录列表
     */
    @GetMapping("/requests")
    public ResponseEntity<List<RequestRecord>> getRequests() {
        return ResponseEntity.ok(modeService.getRequestLog());
    }

    /**
     * 将模式字符串解析为 SimulatorMode 枚举。
     *
     * @param modeStr 模式字符串
     * @return 对应的 SimulatorMode 枚举值
     * @throws IllegalArgumentException 如果模式字符串无效
     */
    private SimulatorModeService.SimulatorMode parseMode(String modeStr) {
        return switch (modeStr.toLowerCase()) {
            case "normal" -> SimulatorModeService.SimulatorMode.NORMAL;
            case "rate_limited" -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
            case "fault", "upstream_error" -> SimulatorModeService.SimulatorMode.UPSTREAM_ERROR;
            case "auth_error" -> SimulatorModeService.SimulatorMode.AUTH_ERROR;
            case "quota_exceeded" -> SimulatorModeService.SimulatorMode.QUOTA_EXCEEDED;
            case "invalid_request" -> SimulatorModeService.SimulatorMode.INVALID_REQUEST;
            case "service_down" -> SimulatorModeService.SimulatorMode.SERVICE_DOWN;
            case "timeout" -> SimulatorModeService.SimulatorMode.TIMEOUT;
            case "intermittent" -> SimulatorModeService.SimulatorMode.INTERMITTENT;
            default -> throw new IllegalArgumentException("不支持的模式: " + modeStr);
        };
    }

    // ==================== 行为序列管理 ====================

    /**
     * 设置行为序列。
     *
     * @param body 包含 steps（状态码列表）和 loop（是否循环）的请求体
     * @return 操作结果
     */
    @PostMapping("/behavior")
    public ResponseEntity<Map<String, Object>> setBehavior(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> steps = ((List<Object>) body.getOrDefault("steps", List.of()))
                .stream()
                .map(o -> o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(o.toString()))
                .collect(Collectors.toList());
        boolean loop = Boolean.TRUE.equals(body.get("loop"));
        modeService.setBehaviorSequence(steps, loop);
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "steps", steps,
                "loop", loop
        ));
    }

    /**
     * 获取当前行为序列。
     *
     * @return 行为序列信息
     */
    @GetMapping("/behavior")
    public ResponseEntity<Map<String, Object>> getBehavior() {
        BehaviorSequence seq = modeService.getBehaviorSequence();
        if (seq == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        return ResponseEntity.ok(Map.of(
                "active", seq.isActive(),
                "loop", seq.isLoop(),
                "steps", seq.getSteps(),
                "currentIndex", seq.getCurrentIndex()
        ));
    }

    /**
     * 清除行为序列。
     *
     * @return 操作结果
     */
    @DeleteMapping("/behavior")
    public ResponseEntity<Map<String, String>> clearBehavior() {
        modeService.clearBehaviorSequence();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ==================== 延迟配置管理 ====================

    /**
     * 设置延迟。
     *
     * @param body 包含 delayMs 的请求体
     * @return 操作结果
     */
    @PostMapping("/delay")
    public ResponseEntity<Map<String, Object>> setDelay(@RequestBody Map<String, Object> body) {
        long delayMs = body.containsKey("delayMs") ? ((Number) body.get("delayMs")).longValue() : 0;
        modeService.getDelayConfig().setDelay(delayMs);
        return ResponseEntity.ok(Map.of("status", "ok", "delayMs", delayMs));
    }

    /**
     * 获取当前延迟配置。
     *
     * @return 延迟配置
     */
    @GetMapping("/delay")
    public ResponseEntity<Map<String, Object>> getDelay() {
        return ResponseEntity.ok(Map.of(
                "active", modeService.getDelayConfig().isActive(),
                "delayMs", modeService.getDelayConfig().getFixedDelayMs()
        ));
    }

    /**
     * 清除延迟配置。
     *
     * @return 操作结果
     */
    @DeleteMapping("/delay")
    public ResponseEntity<Map<String, String>> clearDelay() {
        modeService.getDelayConfig().clearDelay();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ==================== 流控制配置管理 ====================

    /**
     * 配置流行为。
     *
     * @param body 流配置参数
     * @return 操作结果
     */
    @PostMapping("/stream")
    public ResponseEntity<Map<String, Object>> setStream(@RequestBody Map<String, Object> body) {
        String action = (String) body.getOrDefault("action", "normal");
        int chunkCount = body.containsKey("chunkCount") ? ((Number) body.get("chunkCount")).intValue() : 3;
        int chunkIntervalMs = body.containsKey("chunkIntervalMs") ? ((Number) body.get("chunkIntervalMs")).intValue() : 50;
        int interruptAfter = body.containsKey("interruptAfter") ? ((Number) body.get("interruptAfter")).intValue() : 0;
        String invalidChunk = (String) body.getOrDefault("invalidChunk", "");
        modeService.getStreamConfig().configure(action, chunkCount, chunkIntervalMs, interruptAfter, invalidChunk);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * 获取当前流配置。
     *
     * @return 流配置
     */
    @GetMapping("/stream")
    public ResponseEntity<Map<String, Object>> getStream() {
        return ResponseEntity.ok(Map.of(
                "action", modeService.getStreamConfig().getAction(),
                "chunkCount", modeService.getStreamConfig().getChunkCount(),
                "chunkIntervalMs", modeService.getStreamConfig().getChunkIntervalMs(),
                "interruptAfter", modeService.getStreamConfig().getInterruptAfter()
        ));
    }

    /**
     * 重置流配置。
     *
     * @return 操作结果
     */
    @DeleteMapping("/stream")
    public ResponseEntity<Map<String, String>> resetStream() {
        modeService.getStreamConfig().reset();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ==================== API Key 覆盖管理 ====================

    /**
     * 设置 API Key 前缀覆盖。
     *
     * @param body 包含 keyPrefix、status 和 body 的请求体
     * @return 操作结果
     */
    @PostMapping("/apikey-override")
    public ResponseEntity<Map<String, Object>> setApiKeyOverride(@RequestBody Map<String, Object> body) {
        String keyPrefix = (String) body.get("keyPrefix");
        if (keyPrefix == null || keyPrefix.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 keyPrefix"));
        }
        int status = body.containsKey("status") ? ((Number) body.get("status")).intValue() : 401;
        SimulatorModeService.SimulatorMode mode = httpStatusToMode(status);
        modeService.getApiKeyOverrideConfig().setOverride(keyPrefix, mode);
        return ResponseEntity.ok(Map.of("status", "ok", "keyPrefix", keyPrefix, "mode", mode.name()));
    }

    /**
     * 移除 API Key 前缀覆盖。
     *
     * @param body 包含 keyPrefix 的请求体
     * @return 操作结果
     */
    @DeleteMapping("/apikey-override")
    public ResponseEntity<Map<String, Object>> removeApiKeyOverride(@RequestBody Map<String, Object> body) {
        String keyPrefix = (String) body.get("keyPrefix");
        if (keyPrefix != null) {
            modeService.getApiKeyOverrideConfig().removeOverride(keyPrefix);
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    /**
     * 获取所有 API Key 覆盖规则。
     *
     * @return 覆盖规则列表
     */
    @GetMapping("/apikey-override")
    public ResponseEntity<Map<String, Object>> getApiKeyOverrides() {
        return ResponseEntity.ok(Map.of("overrides", modeService.getApiKeyOverrideConfig().getOverrides()));
    }

    /**
     * HTTP 状态码到 SimulatorMode 的映射。
     *
     * @param statusCode HTTP 状态码
     * @return 对应的 SimulatorMode
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
