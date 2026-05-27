package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelModelService;
import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道模型关联 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/models")
@RequiredArgsConstructor
public class ChannelModelController {

    private final ChannelModelService channelModelService;

    @GetMapping
    public ResponseEntity<List<ChannelModelResponse>> list(@PathVariable Long channelId) {
        return ResponseEntity.ok(channelModelService.getModelsByChannelId(channelId));
    }

    @PostMapping
    public ResponseEntity<ChannelModelResponse> create(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelModelCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(channelModelService.create(channelId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long channelId, @PathVariable Long id) {
        channelModelService.delete(channelId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<Void> setEnabled(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        channelModelService.setEnabled(channelId, id, enabled);
        return ResponseEntity.noContent().build();
    }
}