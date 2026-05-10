package com.codingas.gateway.infrastructure.template.database;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Provider 模板 Repository
 *
 * <p>提供 Provider 模板的数据访问操作。</p>
 */
@Repository
public interface ProviderTemplateRepository extends JpaRepository<ProviderTemplateDo, Long> {

    /**
     * 根据模板编码查询（排除已删除）
     *
     * @param code 模板编码
     * @return 模板 DO
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.templateCode = :code AND t.deletedAt IS NULL")
    Optional<ProviderTemplateDo> findByTemplateCode(@Param("code") String code);

    /**
     * 查询所有官方模板（排除已删除）
     *
     * @return 官方模板列表
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.templateType = 'OFFICIAL' AND t.deletedAt IS NULL AND t.status = 'ACTIVE'")
    List<ProviderTemplateDo> findOfficialTemplates();

    /**
     * 分页查询公共市场模板
     *
     * @param pageable 分页参数
     * @return 市场模板分页结果
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.marketStatus = 'PUBLISHED' AND t.deletedAt IS NULL AND t.status = 'ACTIVE'")
    Page<ProviderTemplateDo> findMarketTemplates(Pageable pageable);

    /**
     * 根据作者查询
     *
     * @param authorId 作者 ID
     * @return 作者的模板列表
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.authorId = :authorId AND t.deletedAt IS NULL")
    List<ProviderTemplateDo> findByAuthorId(@Param("authorId") Long authorId);

    /**
     * 检查模板编码是否存在
     *
     * @param code 模板编码
     * @return 是否存在
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM ProviderTemplateDo t WHERE t.templateCode = :code AND t.deletedAt IS NULL")
    boolean existsByTemplateCode(@Param("code") String code);

    /**
     * 软删除
     *
     * @param id 模板 ID
     * @param deletedAt 删除时间
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.deletedAt = :deletedAt WHERE t.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") Instant deletedAt);

    /**
     * 更新市场状态
     *
     * @param id 模板 ID
     * @param status 市场状态
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.marketStatus = :status WHERE t.id = :id")
    void updateMarketStatus(@Param("id") Long id, @Param("status") MarketStatus status);

    /**
     * 增加使用次数
     *
     * @param id 模板 ID
     */
    @Modifying
    @Query("UPDATE ProviderTemplateDo t SET t.downloadCount = t.downloadCount + 1 WHERE t.id = :id")
    void incrementDownloadCount(@Param("id") Long id);

    /**
     * 动态条件查询
     *
     * @param templateType 模板类型
     * @param providerType 提供商类型
     * @param keyword 关键词
     * @param marketStatus 市场状态
     * @param pageable 分页参数
     * @return 符合条件的模板分页结果
     */
    @Query("SELECT t FROM ProviderTemplateDo t WHERE t.deletedAt IS NULL " +
           "AND (:templateType IS NULL OR t.templateType = :templateType) " +
           "AND (:providerType IS NULL OR t.providerType = :providerType) " +
           "AND (:keyword IS NULL OR LOWER(t.templateName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:marketStatus IS NULL OR t.marketStatus = :marketStatus)")
    Page<ProviderTemplateDo> findByConditions(
        @Param("templateType") ProviderTemplateDo.TemplateType templateType,
        @Param("providerType") String providerType,
        @Param("keyword") String keyword,
        @Param("marketStatus") MarketStatus marketStatus,
        Pageable pageable
    );
}
