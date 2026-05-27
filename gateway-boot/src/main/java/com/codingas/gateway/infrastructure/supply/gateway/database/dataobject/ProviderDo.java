package com.codingas.gateway.infrastructure.supply.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "providers")
public class ProviderDo extends BaseDo {

    @Column(name = "provider_id", unique = true, length = 64)
    private String code;

    @Column(name = "provider_name", nullable = false, length = 128)
    private String name;

    @Column(name = "icon_url", length = 512)
    private String logoUrl;

    @Column(name = "website_url", length = 512)
    private String websiteUrl;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "state", nullable = false)
    private String state;
}