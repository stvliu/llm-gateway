package com.codingas.gateway.application.degradation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能降级配置
 */
@ConfigurationProperties(prefix = "gateway.degradation")
public class DegradationProperties {

    private boolean enabled = true;
    private int maxChainDepth = 5;
    private List<DegradationChain> chains = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxChainDepth() { return maxChainDepth; }
    public void setMaxChainDepth(int maxChainDepth) { this.maxChainDepth = maxChainDepth; }

    public List<DegradationChain> getChains() { return chains; }
    public void setChains(List<DegradationChain> chains) { this.chains = chains; }

    public static class DegradationChain {
        private String primary;
        private List<String> fallbacks = new ArrayList<>();
        private RecoveryConfig recovery = new RecoveryConfig();

        public String getPrimary() { return primary; }
        public void setPrimary(String primary) { this.primary = primary; }

        public List<String> getFallbacks() { return fallbacks; }
        public void setFallbacks(List<String> fallbacks) { this.fallbacks = fallbacks; }

        public RecoveryConfig getRecovery() { return recovery; }
        public void setRecovery(RecoveryConfig recovery) { this.recovery = recovery; }
    }

    public static class RecoveryConfig {
        private Duration checkInterval = Duration.ofSeconds(60);
        private int successThreshold = 3;

        public Duration getCheckInterval() { return checkInterval; }
        public void setCheckInterval(Duration checkInterval) { this.checkInterval = checkInterval; }

        public int getSuccessThreshold() { return successThreshold; }
        public void setSuccessThreshold(int successThreshold) { this.successThreshold = successThreshold; }
    }
}
