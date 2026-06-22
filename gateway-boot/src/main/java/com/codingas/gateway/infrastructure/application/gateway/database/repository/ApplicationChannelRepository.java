package com.codingas.gateway.infrastructure.application.gateway.database.repository;

import com.codingas.gateway.infrastructure.application.gateway.database.dataobject.ApplicationChannelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 应用-渠道授权关联 JPA Repository
 */
public interface ApplicationChannelRepository extends JpaRepository<ApplicationChannelDo, Long> {

    /**
     * 查询应用可见的渠道 ID 列表（DISTINCT 去重）
     *
     * @param appId 应用 ID
     * @return 渠道 ID 列表
     */
    @Query("SELECT DISTINCT ac.channelId FROM ApplicationChannelDo ac WHERE ac.applicationId = :appId")
    List<Long> findChannelIdsByApplicationId(@Param("appId") Long appId);

    /**
     * 查询应用下的全部授权关联
     *
     * @param applicationId 应用 ID
     * @return 关联 DO 列表
     */
    List<ApplicationChannelDo> findByApplicationId(Long applicationId);

    /**
     * 判断应用-渠道授权关联是否存在
     *
     * @param applicationId 应用 ID
     * @param channelId     渠道 ID
     * @return 存在返回 true
     */
    boolean existsByApplicationIdAndChannelId(Long applicationId, Long channelId);

    /**
     * 删除应用下的全部授权关联
     *
     * @param applicationId 应用 ID
     */
    @Modifying
    @Query("DELETE FROM ApplicationChannelDo ac WHERE ac.applicationId = :applicationId")
    void deleteByApplicationId(@Param("applicationId") Long applicationId);
}
