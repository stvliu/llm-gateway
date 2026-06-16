package com.codingas.simulator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimulatorModeService 单元测试。
 * <p>
 * 验证模式切换、请求记录和环形缓冲区功能。
 */
class SimulatorModeServiceTest {

    private SimulatorModeService service;

    @BeforeEach
    void setUp() {
        service = new SimulatorModeService("normal", 5);
    }

    @Test
    @DisplayName("默认模式为 NORMAL")
    void defaultMode_isNormal() {
        assertEquals(SimulatorModeService.SimulatorMode.NORMAL, service.getMode());
    }

    @Test
    @DisplayName("可以切换模式为 RATE_LIMITED")
    void setMode_rateLimited() {
        service.setMode(SimulatorModeService.SimulatorMode.RATE_LIMITED);
        assertEquals(SimulatorModeService.SimulatorMode.RATE_LIMITED, service.getMode());
    }

    @Test
    @DisplayName("可以切换模式为 FAULT")
    void setMode_fault() {
        service.setMode(SimulatorModeService.SimulatorMode.FAULT);
        assertEquals(SimulatorModeService.SimulatorMode.FAULT, service.getMode());
    }

    @Test
    @DisplayName("记录请求后可以查询到记录")
    void recordRequest_andRetrieve() {
        service.recordRequest("POST", "/v1/chat/completions");

        List<RequestRecord> log = service.getRequestLog();
        assertEquals(1, log.size());
        assertEquals("POST", log.get(0).method());
        assertEquals("/v1/chat/completions", log.get(0).path());
        assertNotNull(log.get(0).timestamp());
    }

    @Test
    @DisplayName("多条请求记录按顺序保存")
    void recordMultipleRequests() {
        service.recordRequest("POST", "/v1/chat/completions");
        service.recordRequest("POST", "/v1/messages");

        List<RequestRecord> log = service.getRequestLog();
        assertEquals(2, log.size());
        assertEquals("/v1/chat/completions", log.get(0).path());
        assertEquals("/v1/messages", log.get(1).path());
    }

    @Test
    @DisplayName("环形缓冲区超出容量时丢弃最旧记录")
    void ringBuffer_dropsOldestWhenFull() {
        // 容量为 5
        for (int i = 0; i < 7; i++) {
            service.recordRequest("GET", "/path/" + i);
        }

        List<RequestRecord> log = service.getRequestLog();
        assertEquals(5, log.size(), "应保留最新 5 条记录");
        assertEquals("/path/2", log.get(0).path(), "最早的记录应被丢弃");
        assertEquals("/path/6", log.get(4).path(), "最新的记录应在末尾");
    }

    @Test
    @DisplayName("构造时根据配置字符串设置初始模式")
    void constructor_setsModeFromConfig() {
        SimulatorModeService rateLimitedService = new SimulatorModeService("rate_limited", 100);
        assertEquals(SimulatorModeService.SimulatorMode.RATE_LIMITED, rateLimitedService.getMode());

        SimulatorModeService faultService = new SimulatorModeService("fault", 100);
        assertEquals(SimulatorModeService.SimulatorMode.FAULT, faultService.getMode());
    }
}
