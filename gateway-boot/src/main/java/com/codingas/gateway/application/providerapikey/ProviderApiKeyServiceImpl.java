package com.codingas.gateway.application.providerapikey;

import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyQueryRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.model.entity.ProviderApiKey;
import com.codingas.gateway.domain.model.entity.ProviderApiKey.ProviderApiKeyStatus;
import com.codingas.gateway.domain.model.gateway.ProviderGateway;
import com.codingas.gateway.domain.model.gateway.ProviderApiKeyGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provider API Key 应用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderApiKeyServiceImpl implements ProviderApiKeyService {

    private final ProviderApiKeyGateway providerApiKeyGateway;
    private final ProviderGateway providerGateway;

    @Override
    @Transactional
    public ProviderApiKeyCreateResponse create(ProviderApiKeyCreateRequest request) {
        providerGateway.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", request.getProviderId()));

        ProviderApiKey key = new ProviderApiKey();
        key.setProviderId(request.getProviderId());
        key.setKeyName(request.getKeyName());
        key.setApiKey(request.getApiKey());
        key.setPriority(request.getPriority());
        key.setWeight(request.getWeight());
        key.setIsDefault(request.getIsDefault());
        key.setExpiresAt(request.getExpiresAt());
        key.setStatus(ProviderApiKeyStatus.ACTIVE);

        ProviderApiKey saved = providerApiKeyGateway.save(key);
        log.info("Created ProviderApiKey: id={}, providerId={}", saved.getId(), saved.getProviderId());

        return ProviderApiKeyCreateResponse.from(saved, request.getApiKey());
    }

    @Override
    public ProviderApiKeyResponse getById(Long id) {
        ProviderApiKey key = providerApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderApiKey", id));
        return ProviderApiKeyResponse.from(key);
    }

    @Override
    public PageResponse<ProviderApiKeyResponse> query(ProviderApiKeyQueryRequest request) {
        if (request.getProviderId() == null) {
            return PageResponse.of(List.of(), request.getPage(), request.getLimit(), 0);
        }

        int page = request.getPage() != null ? request.getPage() : 1;
        int limit = request.getLimit() != null ? request.getLimit() : 20;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "id"));

        Page<ProviderApiKey> keyPage;
        if (request.getStatus() != null && request.getKeyword() != null && !request.getKeyword().isBlank()) {
            keyPage = providerApiKeyGateway.findByProviderIdAndStatusAndKeyword(
                request.getProviderId(), request.getStatus(), request.getKeyword(), pageable);
        } else if (request.getStatus() != null) {
            keyPage = providerApiKeyGateway.findByProviderIdAndStatus(
                request.getProviderId(), request.getStatus(), pageable);
        } else if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            keyPage = providerApiKeyGateway.findByProviderIdAndKeyword(
                request.getProviderId(), request.getKeyword(), pageable);
        } else {
            keyPage = providerApiKeyGateway.findByProviderId(request.getProviderId(), pageable);
        }

        List<ProviderApiKeyResponse> responses = keyPage.getContent().stream()
                .map(ProviderApiKeyResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, page, limit, keyPage.getTotalElements());
    }

    @Override
    @Transactional
    public ProviderApiKeyResponse update(Long id, ProviderApiKeyUpdateRequest request) {
        ProviderApiKey key = providerApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderApiKey", id));

        if (request.getKeyName() != null) {
            key.setKeyName(request.getKeyName());
        }
        if (request.getApiKey() != null) {
            key.setApiKey(request.getApiKey());
        }
        if (request.getPriority() != null) {
            key.setPriority(request.getPriority());
        }
        if (request.getWeight() != null) {
            key.setWeight(request.getWeight());
        }
        if (request.getIsDefault() != null) {
            key.setIsDefault(request.getIsDefault());
        }
        if (request.getStatus() != null) {
            key.setStatus(request.getStatus());
        }
        if (request.getExpiresAt() != null) {
            key.setExpiresAt(request.getExpiresAt());
        }
        if (request.getRpmLimit() != null) {
            key.setRpmLimit(request.getRpmLimit());
        }
        if (request.getTpmLimit() != null) {
            key.setTpmLimit(request.getTpmLimit());
        }

        ProviderApiKey saved = providerApiKeyGateway.save(key);
        log.info("Updated ProviderApiKey: id={}", saved.getId());

        return ProviderApiKeyResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProviderApiKey key = providerApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderApiKey", id));

        key.setStatus(ProviderApiKeyStatus.DELETED);
        providerApiKeyGateway.save(key);
        log.info("Deleted ProviderApiKey: id={}", id);
    }

    @Override
    @Transactional
    public ProviderApiKeyResponse setEnabled(Long id, boolean enabled) {
        ProviderApiKey key = providerApiKeyGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProviderApiKey", id));

        key.setStatus(enabled ? ProviderApiKeyStatus.ACTIVE : ProviderApiKeyStatus.DISABLED);
        ProviderApiKey saved = providerApiKeyGateway.save(key);
        log.info("Set ProviderApiKey enabled={}: id={}", enabled, id);

        return ProviderApiKeyResponse.from(saved);
    }
}