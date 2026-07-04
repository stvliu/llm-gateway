package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.resilience.ResilienceEventService;
import com.codingas.gateway.application.resilience.dto.FailoverEventResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * 容灾事件查询 REST 控制器
 *
 * <p>提供转移事件流的轮询查询端点（容灾可观测性，读侧重）。设计见 design doc D12：
 * 前端总览页 10s 轮询渲染转移事件流 + 耗尽告警。</p>
 *
 * <ul>
 *   <li>GET /api/v1/resilience/events — 转移事件流查询（分页 + since/applicationId 过滤）</li>
 *   <li>GET /api/v1/resilience/events/exhausted — 耗尽告警查询（exhausted=true 近期事件）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/resilience/events")
@RequiredArgsConstructor
public class ResilienceEventController {

    /** 转移事件流默认返回条数 */
    private static final int DEFAULT_LIMIT = 100;
    /** 转移事件流返回条数上限 */
    private static final int MAX_LIMIT = 500;
    /** 耗尽告警默认返回条数 */
    private static final int DEFAULT_EXHAUSTED_LIMIT = 50;

    private final ResilienceEventService resilienceEventService;

    /**
     * 查询转移事件流（按 occurredAt 倒序）
     *
     * @param since         起始时间过滤（可选，ISO-8601 Instant）
     * @param applicationId 应用 ID 过滤（可选）
     * @param limit         返回条数（默认 100，上限 500）
     * @return 转移事件响应列表
     */
    @GetMapping
    public List<FailoverEventResponse> list(
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        int cappedLimit = capLimit(limit, DEFAULT_LIMIT, MAX_LIMIT);
        return resilienceEventService.findRecent(since, applicationId, cappedLimit);
    }

    /**
     * 查询耗尽告警事件（exhausted=true，按 occurredAt 倒序）
     *
     * @param since 起始时间过滤（可选，ISO-8601 Instant；不传时由 Service 层补默认窗口最近 1 小时）
     * @param limit 返回条数（默认 50）
     * @return 耗尽事件响应列表
     */
    @GetMapping("/exhausted")
    public List<FailoverEventResponse> exhausted(
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        int cappedLimit = capLimit(limit, DEFAULT_EXHAUSTED_LIMIT, MAX_LIMIT);
        return resilienceEventService.findExhausted(since, cappedLimit);
    }

    /**
     * 限制 limit 在合理范围：null 用默认值，超过上限截断为上限
     */
    private int capLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }
}
