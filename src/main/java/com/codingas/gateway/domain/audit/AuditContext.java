package com.codingas.gateway.domain.audit;

/**
 * 审计上下文
 *
 * <p>用于传递审计所需的信息。</p>
 */
public record AuditContext(
    Long userId,
    String action,
    String resource,
    String requestMethod,
    String requestPath,
    String requestBody,
    Integer responseStatus,
    Integer responseTime,
    String traceId,
    String ipAddress,
    String userAgent,
    String errorMessage
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String action;
        private String resource;
        private String requestMethod;
        private String requestPath;
        private String requestBody;
        private Integer responseStatus;
        private Integer responseTime;
        private String traceId;
        private String ipAddress;
        private String userAgent;
        private String errorMessage;

        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder action(String action) { this.action = action; return this; }
        public Builder resource(String resource) { this.resource = resource; return this; }
        public Builder requestMethod(String requestMethod) { this.requestMethod = requestMethod; return this; }
        public Builder requestPath(String requestPath) { this.requestPath = requestPath; return this; }
        public Builder requestBody(String requestBody) { this.requestBody = requestBody; return this; }
        public Builder responseStatus(Integer responseStatus) { this.responseStatus = responseStatus; return this; }
        public Builder responseTime(Integer responseTime) { this.responseTime = responseTime; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public AuditContext build() {
            return new AuditContext(userId, action, resource, requestMethod, requestPath,
                requestBody, responseStatus, responseTime, traceId, ipAddress, userAgent, errorMessage);
        }
    }
}
