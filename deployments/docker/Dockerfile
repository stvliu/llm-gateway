# ===================================================================
# LLM-Gateway Dockerfile
# 多阶段构建，优化镜像大小
# ===================================================================

# ===================================================================
# 第一阶段: 构建
# ===================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 安装 Maven
RUN apk add --no-cache maven

# 复制 POM 文件
COPY gateway/pom.xml .
COPY gateway/gateway-app/pom.xml gateway-app/
COPY gateway/gateway-web/pom.xml gateway-web/
COPY gateway/gateway-core/pom.xml gateway-core/
COPY gateway/gateway-adapter/pom.xml gateway-adapter/

# 下载依赖 (利用缓存)
RUN maven dependency:go-offline -pl gateway-app,gateway-web,gateway-core,gateway-adapter -am

# 复制源代码
COPY gateway/gateway-app/src gateway-app/src
COPY gateway/gateway-web/src gateway-web/src
COPY gateway/gateway-core/src gateway-core/src
COPY gateway/gateway-adapter/src gateway-adapter/src

# 构建
ARG SPRING_PROFILES_ACTIVE=dev
RUN maven package -DskipTests -pl gateway-app -am -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}

# ===================================================================
# 第二阶段: 运行
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 curl (用于健康检查)
RUN apk add --no-cache curl bash

# 创建非 root 用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 复制 JAR 文件
COPY --from=builder /build/gateway-app/target/*.jar app.jar

# 复制配置文件
COPY deployments/docker/config /app/config

# 设置权限
RUN chown -R appuser:appgroup /app

USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1

# 默认暴露端口
EXPOSE 8080 8081

# 启动命令
ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "-XX:+ZGenerational", "app.jar"]
