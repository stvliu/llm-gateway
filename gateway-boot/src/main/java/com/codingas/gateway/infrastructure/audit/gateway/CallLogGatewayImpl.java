package com.codingas.gateway.infrastructure.audit.gateway;

import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.audit.gateway.CallLogGateway;
import com.codingas.gateway.infrastructure.audit.gateway.database.dataobject.CallLogDo;
import com.codingas.gateway.infrastructure.audit.gateway.database.repository.CallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 调用日志 Gateway 实现
 */
@Component
@RequiredArgsConstructor
public class CallLogGatewayImpl implements CallLogGateway {

    private final CallLogRepository repository;

    @Override
    public CallLog save(CallLog callLog) {
        CallLogDo do_ = toDo(callLog);
        CallLogDo saved = repository.save(do_);
        return toEntity(saved);
    }

    @Override
    public Optional<CallLog> findByTraceId(String traceId) {
        return repository.findByTraceId(traceId).map(this::toEntity);
    }

    @Override
    public List<CallLog> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toEntity).toList();
    }

    private CallLogDo toDo(CallLog entity) {
        CallLogDo do_ = new CallLogDo();
        do_.setTraceId(entity.getTraceId());
        do_.setUserId(entity.getUserId());
        do_.setModel(entity.getModel());
        do_.setChannelId(entity.getChannelId());
        do_.setChannelEndpointId(entity.getChannelEndpointId());
        do_.setInboundProtocol(entity.getInboundProtocol());
        do_.setUpstreamProtocol(entity.getUpstreamProtocol());
        do_.setDurationMs(entity.getDurationMs());
        do_.setSuccess(entity.getSuccess());
        do_.setInputTokens(entity.getInputTokens());
        do_.setOutputTokens(entity.getOutputTokens());
        do_.setErrorMessage(entity.getErrorMessage());
        do_.setCalledAt(entity.getCalledAt());
        return do_;
    }

    private CallLog toEntity(CallLogDo do_) {
        CallLog entity = new CallLog();
        entity.setId(do_.getId());
        entity.setTraceId(do_.getTraceId());
        entity.setUserId(do_.getUserId());
        entity.setModel(do_.getModel());
        entity.setChannelId(do_.getChannelId());
        entity.setChannelEndpointId(do_.getChannelEndpointId());
        entity.setInboundProtocol(do_.getInboundProtocol());
        entity.setUpstreamProtocol(do_.getUpstreamProtocol());
        entity.setDurationMs(do_.getDurationMs());
        entity.setSuccess(do_.getSuccess());
        entity.setInputTokens(do_.getInputTokens());
        entity.setOutputTokens(do_.getOutputTokens());
        entity.setErrorMessage(do_.getErrorMessage());
        entity.setCalledAt(do_.getCalledAt());
        return entity;
    }
}