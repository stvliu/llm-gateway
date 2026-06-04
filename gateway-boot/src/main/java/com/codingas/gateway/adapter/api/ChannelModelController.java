package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.channel.ChannelModelService;
import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 渠道模型关联 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/channels/{channelId}/models")
@RequiredArgsConstructor
public class ChannelModelController {

    private final ChannelModelService channelModelService;

    @GetMapping
    public List<ChannelModelResponse> list(@PathVariable Long channelId) {
        return channelModelService.getModelsByChannelId(channelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelModelResponse create(
            @PathVariable Long channelId,
            @Valid @RequestBody ChannelModelCreateRequest request) {
        return channelModelService.create(channelId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long channelId, @PathVariable Long id) {
        channelModelService.delete(channelId, id);
    }

    @PatchMapping("/{id}/state")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEnabled(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        channelModelService.setEnabled(channelId, id, enabled);
    }

    @PatchMapping("/{id}/upstream-model-name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUpstreamModelName(
            @PathVariable Long channelId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String upstreamModelName = body.get("upstreamModelName");
        channelModelService.updateUpstreamModelName(channelId, id, upstreamModelName);
    }
}
