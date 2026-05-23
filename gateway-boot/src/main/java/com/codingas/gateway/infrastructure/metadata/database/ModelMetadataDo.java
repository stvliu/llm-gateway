package com.codingas.gateway.infrastructure.metadata.database;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 模型元数据 DO
 */
@Entity
@Table(name = "model_metadata")
public class ModelMetadataDo extends BaseDo {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "provider_model_id", nullable = false, length = 128)
    private String providerModelId;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "model_family", length = 64)
    private String modelFamily;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_input_tokens")
    private Integer maxInputTokens;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    @Column(name = "input_price", precision = 12, scale = 6)
    private BigDecimal inputPrice;

    @Column(name = "output_price", precision = 12, scale = 6)
    private BigDecimal outputPrice;

    @Column(name = "reasoning_price", precision = 12, scale = 6)
    private BigDecimal reasoningPrice;

    @Column(name = "cache_read_price", precision = 12, scale = 6)
    private BigDecimal cacheReadPrice;

    @Column(name = "cache_write_price", precision = 12, scale = 6)
    private BigDecimal cacheWritePrice;

    @Column(name = "input_audio_price", precision = 12, scale = 6)
    private BigDecimal inputAudioPrice;

    @Column(name = "output_audio_price", precision = 12, scale = 6)
    private BigDecimal outputAudioPrice;

    @Column(name = "knowledge_cutoff", length = 32)
    private String knowledgeCutoff;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "open_weights")
    private Boolean openWeights;

    @Column(name = "modalities", columnDefinition = "json")
    private String modalities;

    @Column(name = "capabilities", columnDefinition = "json")
    private String capabilities;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "source_synced_at")
    private Instant sourceSyncedAt;

    @Column(name = "state", nullable = false, length = 32)
    private String state;

    // ==================== Getter / Setter ====================

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderModelId() { return providerModelId; }
    public void setProviderModelId(String providerModelId) { this.providerModelId = providerModelId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getModelFamily() { return modelFamily; }
    public void setModelFamily(String modelFamily) { this.modelFamily = modelFamily; }

    public Integer getContextWindow() { return contextWindow; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }

    public Integer getMaxInputTokens() { return maxInputTokens; }
    public void setMaxInputTokens(Integer maxInputTokens) { this.maxInputTokens = maxInputTokens; }

    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public BigDecimal getInputPrice() { return inputPrice; }
    public void setInputPrice(BigDecimal inputPrice) { this.inputPrice = inputPrice; }

    public BigDecimal getOutputPrice() { return outputPrice; }
    public void setOutputPrice(BigDecimal outputPrice) { this.outputPrice = outputPrice; }

    public BigDecimal getReasoningPrice() { return reasoningPrice; }
    public void setReasoningPrice(BigDecimal reasoningPrice) { this.reasoningPrice = reasoningPrice; }

    public BigDecimal getCacheReadPrice() { return cacheReadPrice; }
    public void setCacheReadPrice(BigDecimal cacheReadPrice) { this.cacheReadPrice = cacheReadPrice; }

    public BigDecimal getCacheWritePrice() { return cacheWritePrice; }
    public void setCacheWritePrice(BigDecimal cacheWritePrice) { this.cacheWritePrice = cacheWritePrice; }

    public BigDecimal getInputAudioPrice() { return inputAudioPrice; }
    public void setInputAudioPrice(BigDecimal inputAudioPrice) { this.inputAudioPrice = inputAudioPrice; }

    public BigDecimal getOutputAudioPrice() { return outputAudioPrice; }
    public void setOutputAudioPrice(BigDecimal outputAudioPrice) { this.outputAudioPrice = outputAudioPrice; }

    public String getKnowledgeCutoff() { return knowledgeCutoff; }
    public void setKnowledgeCutoff(String knowledgeCutoff) { this.knowledgeCutoff = knowledgeCutoff; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public Boolean getOpenWeights() { return openWeights; }
    public void setOpenWeights(Boolean openWeights) { this.openWeights = openWeights; }

    public String getModalities() { return modalities; }
    public void setModalities(String modalities) { this.modalities = modalities; }

    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getSourceSyncedAt() { return sourceSyncedAt; }
    public void setSourceSyncedAt(Instant sourceSyncedAt) { this.sourceSyncedAt = sourceSyncedAt; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
