package com.codingas.gateway.adapter.api.supply;

import com.codingas.gateway.application.supply.service.ChannelBatchService;
import com.codingas.gateway.application.supply.service.ChannelOperationLogService;
import com.codingas.gateway.application.supply.service.ChannelService;
import com.codingas.gateway.application.supply.service.ChannelTestService;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 渠道管理 Controller
 *
 * <p>提供渠道的 CRUD、启停、测试、复制、批量操作等 REST API。</p>
 */
@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelService channelService;
    private final ChannelBatchService channelBatchService;
    private final ChannelOperationLogService operationLogService;
    private final ChannelTestService channelTestService;
    private final ChannelGateway channelGateway;

    public ChannelController(ChannelService channelService,
                             ChannelBatchService channelBatchService,
                             ChannelOperationLogService operationLogService,
                             ChannelTestService channelTestService,
                             ChannelGateway channelGateway) {
        this.channelService = channelService;
        this.channelBatchService = channelBatchService;
        this.operationLogService = operationLogService;
        this.channelTestService = channelTestService;
        this.channelGateway = channelGateway;
    }

    // ==================== 编辑 ====================

    /**
     * 编辑渠道
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('channel:update')")
    public ResponseEntity<Channel> updateChannel(
            @PathVariable Long id,
            @RequestBody Channel updateReq) {
        // 操作人信息从 SecurityContext 获取（简化写法）
        Long operatorId = getCurrentUserId();
        String operatorName = getCurrentUserName();
        String operatorIp = getCurrentUserIp();

        Channel updated = channelService.update(id, updateReq, operatorId, operatorName, operatorIp);
        return ResponseEntity.ok(updated);
    }

    // ==================== 启用/停用 ====================

    /**
     * 启用渠道
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasPermission('channel:enable')")
    public ResponseEntity<Channel> enableChannel(@PathVariable Long id) {
        Channel channel = channelService.enable(id, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(channel);
    }

    /**
     * 停用渠道
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasPermission('channel:disable')")
    public ResponseEntity<Channel> disableChannel(@PathVariable Long id) {
        Channel channel = channelService.disable(id, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(channel);
    }

    // ==================== 测试 ====================

    /**
     * 测试渠道连通性
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasPermission('channel:test')")
    public ResponseEntity<ChannelTestService.TestResult> testChannel(
            @PathVariable Long id,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) Integer timeout) {
        ChannelTestService.TestResult result = channelTestService.test(
                id, model, timeout, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(result);
    }

    // ==================== 复制 ====================

    /**
     * 复制渠道
     */
    @PostMapping("/{id}/copy")
    @PreAuthorize("hasPermission('channel:copy')")
    public ResponseEntity<Channel> copyChannel(@PathVariable Long id) {
        Channel channel = channelService.copy(id, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(channel);
    }

    // ==================== 删除 ====================

    /**
     * 删除渠道
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('channel:delete')")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        channelService.delete(id, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.noContent().build();
    }

    // ==================== 批量操作 ====================

    /**
     * 批量启用
     */
    @PostMapping("/batch/enable")
    @PreAuthorize("hasPermission('channel:enable')")
    public ResponseEntity<ChannelBatchService.BatchResult> batchEnable(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        ChannelBatchService.BatchResult result = channelBatchService.batchEnable(
                ids, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(result);
    }

    /**
     * 批量停用
     */
    @PostMapping("/batch/disable")
    @PreAuthorize("hasPermission('channel:disable')")
    public ResponseEntity<ChannelBatchService.BatchResult> batchDisable(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        ChannelBatchService.BatchResult result = channelBatchService.batchDisable(
                ids, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(result);
    }

    /**
     * 批量删除
     */
    @PostMapping("/batch/delete")
    @PreAuthorize("hasPermission('channel:delete')")
    public ResponseEntity<ChannelBatchService.BatchResult> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        ChannelBatchService.BatchResult result = channelBatchService.batchDelete(
                ids, getCurrentUserId(), getCurrentUserName(), getCurrentUserIp());
        return ResponseEntity.ok(result);
    }

    // ==================== 操作日志 ====================

    /**
     * 查询指定渠道的操作日志
     */
    @GetMapping("/{id}/operations")
    @PreAuthorize("hasPermission('channel:read')")
    public ResponseEntity<Page<ChannelOperationLog>> listOperations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action) {

        List<ChannelOperationLog> logs;
        long total;

        if (action != null && !action.isEmpty()) {
            List<String> actions = List.of(action.split(","));
            logs = operationLogService.listByChannelAndActions(id, actions, page, size);
            total = operationLogService.countByChannel(id); // 简化：不按 action 过滤总数
        } else {
            logs = operationLogService.listByChannel(id, page, size);
            total = operationLogService.countByChannel(id);
        }

        return ResponseEntity.ok(new PageImpl<>(logs, PageRequest.of(page, size), total));
    }

    /**
     * 查询全局操作日志
     */
    @GetMapping("/operations")
    @PreAuthorize("hasPermission('channel:read')")
    public ResponseEntity<List<ChannelOperationLog>> listGlobalOperations(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (operatorId != null) {
            return ResponseEntity.ok(operationLogService.listByOperator(operatorId, page, size));
        }
        return ResponseEntity.ok(List.of());
    }

    // ==================== 占位方法 ====================
    // TODO: 替换为真实的 SecurityContext 获取方式

    private Long getCurrentUserId() {
        return 0L;
    }

    private String getCurrentUserName() {
        return "system";
    }

    private String getCurrentUserIp() {
        return "127.0.0.1";
    }
}
