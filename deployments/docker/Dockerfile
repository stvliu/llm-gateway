#
# Copyright © 2025-2026 codingas.com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ===================================================================
# LLM-Gateway Dockerfile
# 多阶段构建（COLA Light 单模块 gateway-boot）
# build-docker job context=仓库根，故路径相对根目录
# ===================================================================

# ===================================================================
# 第一阶段: 构建
# ===================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# 安装 Maven
RUN apk add --no-cache maven

# 复制父 POM 与 gateway-boot POM（利用缓存）
COPY pom.xml ./
COPY gateway-boot/pom.xml gateway-boot/
COPY gateway-cli/pom.xml gateway-cli/
COPY gateway-simulator/pom.xml gateway-simulator/

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -pl gateway-boot -am -q || true

# 复制 gateway-boot 源码
COPY gateway-boot/src gateway-boot/src

# 构建 fat jar
ARG SPRING_PROFILES_ACTIVE=dev
RUN mvn package -DskipTests -pl gateway-boot -am \
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}

# ===================================================================
# 第二阶段: 运行
# ===================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 curl（健康检查）与 bash
RUN apk add --no-cache curl bash

# 创建非 root 用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 复制 fat jar（名称与 revision=1.0.0-SNAPSHOT 一致）
COPY --from=builder /build/gateway-boot/target/gateway-boot-1.0.0-SNAPSHOT.jar app.jar

# 设置权限
RUN chown -R appuser:appgroup /app

USER appuser

# 健康检查（actuator health 端点）
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 默认暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "-XX:+UseZGC", "-XX:+ZGenerational", "app.jar"]
