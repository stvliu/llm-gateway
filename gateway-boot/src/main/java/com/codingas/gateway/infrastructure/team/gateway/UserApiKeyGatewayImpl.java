package com.codingas.gateway.infrastructure.team.gateway;

import com.codingas.gateway.domain.team.entity.UserApiKey;
import com.codingas.gateway.domain.team.enums.UserApiKeyState;
import com.codingas.gateway.domain.team.gateway.UserApiKeyGateway;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserApiKeyDo;
import com.codingas.gateway.infrastructure.team.gateway.database.repository.UserApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户 API Key Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserApiKeyGatewayImpl implements UserApiKeyGateway {

    private final UserApiKeyRepository userApiKeyRepository;

    @Override
    public UserApiKey save(UserApiKey apiKey) {
        UserApiKeyDo dataObject = toDataObject(apiKey);
        if (apiKey.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        UserApiKeyDo saved = userApiKeyRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<UserApiKey> findById(Long id) {
        return userApiKeyRepository.findById(id).map(this::toEntity);
    }

    @Override
    public Optional<UserApiKey> findByKeyHash(String keyHash) {
        return userApiKeyRepository.findByKeyHash(keyHash).map(this::toEntity);
    }

    @Override
    public List<UserApiKey> findByTeamId(Long teamId) {
        return userApiKeyRepository.findByTeamId(teamId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<UserApiKey> findByProductId(Long productId) {
        return userApiKeyRepository.findByProductId(productId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        userApiKeyRepository.deleteById(id);
    }

    @Override
    public long countByTeamId(Long teamId) {
        return userApiKeyRepository.countByTeamId(teamId);
    }

    private UserApiKey toEntity(UserApiKeyDo dataObject) {
        UserApiKey entity = new UserApiKey();
        entity.setId(dataObject.getId());
        entity.setTeamId(dataObject.getTeamId());
        entity.setOwnerUserId(dataObject.getOwnerUserId());
        entity.setProductId(dataObject.getProductId());
        entity.setKeyHash(dataObject.getKeyHash());
        entity.setKeyPrefix(dataObject.getKeyPrefix());
        entity.setName(dataObject.getName());
        entity.setModels(dataObject.getModels());
        entity.setQuotaLimit(dataObject.getQuotaLimit());
        entity.setState(UserApiKeyState.fromCode(dataObject.getState()));
        return entity;
    }

    private UserApiKeyDo toDataObject(UserApiKey entity) {
        UserApiKeyDo dataObject = new UserApiKeyDo();
        dataObject.setId(entity.getId());
        dataObject.setTeamId(entity.getTeamId());
        dataObject.setOwnerUserId(entity.getOwnerUserId());
        dataObject.setProductId(entity.getProductId());
        dataObject.setKeyHash(entity.getKeyHash());
        dataObject.setKeyPrefix(entity.getKeyPrefix());
        dataObject.setName(entity.getName());
        dataObject.setModels(entity.getModels());
        dataObject.setQuotaLimit(entity.getQuotaLimit());
        dataObject.setState(entity.getState().getCode());
        return dataObject;
    }
}
