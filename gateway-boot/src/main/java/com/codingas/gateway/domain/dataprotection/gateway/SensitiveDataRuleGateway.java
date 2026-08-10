/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.dataprotection.gateway;

import com.codingas.gateway.domain.dataprotection.entity.SensitiveDataRule;

import java.util.List;
import java.util.Optional;

/**
 * 敏感数据规则网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface SensitiveDataRuleGateway {

    /**
     * 根据规则编码查找
     *
     * @param ruleCode 规则编码
     * @return 规则信息，不存在返回 null
     */
    Optional<SensitiveDataRule> findByRuleCode(String ruleCode);

    /**
     * 查找所有启用的规则
     *
     * @return 启用的规则列表
     */
    List<SensitiveDataRule> findByEnabledTrue();

    /**
     * 根据数据类型查找规则
     *
     * @param dataType 数据类型
     * @return 规则列表
     */
    List<SensitiveDataRule> findByDataType(String dataType);

    /**
     * 检查规则编码是否存在
     *
     * @param ruleCode 规则编码
     * @return 是否存在
     */
    boolean existsByRuleCode(String ruleCode);

    /**
     * 统计规则数量
     *
     * @return 规则数量
     */
    long count();

    /**
     * 保存所有规则
     *
     * @param rules 规则列表
     * @return 保存后的规则列表
     */
    List<SensitiveDataRule> saveAll(List<SensitiveDataRule> rules);

    /**
     * 保存规则
     *
     * @param rule 规则
     * @return 保存后的规则
     */
    SensitiveDataRule save(SensitiveDataRule rule);
}
