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
package com.codingas.gateway.auditdata.calllog;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * 调用日志数据对象
 */
@Entity
@Table(name = "call_logs")
public class CallLogDo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "channel_endpoint_id")
    private Long channelEndpointId;

    @Column(name = "inbound_protocol", length = 32)
    private String inboundProtocol;

    @Column(name = "upstream_protocol", length = 32)
    private String upstreamProtocol;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "called_at", nullable = false)
    private Instant calledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getChannelEndpointId() { return channelEndpointId; }
    public void setChannelEndpointId(Long channelEndpointId) { this.channelEndpointId = channelEndpointId; }
    public String getInboundProtocol() { return inboundProtocol; }
    public void setInboundProtocol(String inboundProtocol) { this.inboundProtocol = inboundProtocol; }
    public String getUpstreamProtocol() { return upstreamProtocol; }
    public void setUpstreamProtocol(String upstreamProtocol) { this.upstreamProtocol = upstreamProtocol; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCalledAt() { return calledAt; }
    public void setCalledAt(Instant calledAt) { this.calledAt = calledAt; }
}
