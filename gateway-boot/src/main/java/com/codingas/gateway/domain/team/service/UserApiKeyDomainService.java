package com.codingas.gateway.domain.team.service;

import com.codingas.gateway.domain.team.entity.UserApiKey;

/**
 * 用户 API Key 领域服务
 * <p>
 * 承载用户 API Key 的业务逻辑，保持实体纯洁性（实体只含 Getter/Setter）。
 * </p>
 */
public class UserApiKeyDomainService {

    /**
     * 检查 API Key 是否有权访问指定模型
     * <p>
     * models 为空表示全部允许。
     * </p>
     *
     * @param apiKey    用户 API Key 实体
     * @param modelName 模型名称
     * @return 是否有权访问
     */
    public boolean canAccessModel(UserApiKey apiKey, String modelName) {
        if (apiKey == null) {
            return false;
        }
        if (apiKey.getModels() == null || apiKey.getModels().isEmpty()) {
            return true;
        }
        return apiKey.getModels().contains(modelName);
    }
}