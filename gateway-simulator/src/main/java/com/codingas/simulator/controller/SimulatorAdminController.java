package com.codingas.simulator.controller;

import com.codingas.simulator.service.RequestRecord;
import com.codingas.simulator.service.SimulatorModeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模拟器管理 API Controller，提供模式切换和请求记录查询功能。
 * <p>
 * 端点前缀为 /simulator，支持以下操作：
 * <ul>
 *   <li>POST /simulator/mode — 切换模拟模式</li>
 *   <li>GET /simulator/mode — 获取当前模式</li>
 *   <li>GET /simulator/requests — 查询请求记录</li>
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
     * @return 切换后的模式
     */
    @PostMapping("/mode")
    public ResponseEntity<Map<String, String>> setMode(@RequestBody Map<String, String> body) {
        String modeStr = body.getOrDefault("mode", "normal");
        SimulatorModeService.SimulatorMode mode = parseMode(modeStr);
        modeService.setMode(mode);
        return ResponseEntity.ok(Map.of("mode", mode.name()));
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
     */
    private SimulatorModeService.SimulatorMode parseMode(String modeStr) {
        return switch (modeStr.toLowerCase()) {
            case "rate_limited" -> SimulatorModeService.SimulatorMode.RATE_LIMITED;
            case "fault" -> SimulatorModeService.SimulatorMode.FAULT;
            default -> SimulatorModeService.SimulatorMode.NORMAL;
        };
    }
}
