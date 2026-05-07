package com.codingas.gateway.domain.template.gateway;

import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.TemplateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Provider 模板网关接口
 *
 * <p>定义模板数据访问操作。</p>
 */
public interface ProviderTemplateGateway {

    /**
     * 保存模板
     */
    ProviderTemplate save(ProviderTemplate template);

    /**
     * 根据 ID 查询模板
     */
    Optional<ProviderTemplate> findById(Long id);

    /**
     * 根据模板编码查询
     */
    Optional<ProviderTemplate> findByTemplateCode(String templateCode);

    /**
     * 分页查询模板
     *
     * @param templateType 模板类型（可选）
     * @param providerType Provider 类型（可选）
     * @param keyword 关键词（可选）
     * @param marketStatus 市场状态（可选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<ProviderTemplate> findByConditions(
        TemplateType templateType,
        String providerType,
        String keyword,
        MarketStatus marketStatus,
        Pageable pageable
    );

    /**
     * 查询所有官方模板
     */
    List<ProviderTemplate> findOfficialTemplates();

    /**
     * 查询公共市场模板
     */
    Page<ProviderTemplate> findMarketTemplates(Pageable pageable);

    /**
     * 根据作者查询模板
     */
    List<ProviderTemplate> findByAuthorId(Long authorId);

    /**
     * 删除模板（软删除）
     */
    void deleteById(Long id);

    /**
     * 检查模板编码是否存在
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * 更新市场状态
     */
    void updateMarketStatus(Long id, MarketStatus marketStatus);

    /**
     * 增加使用次数
     */
    void incrementDownloadCount(Long id);
}
