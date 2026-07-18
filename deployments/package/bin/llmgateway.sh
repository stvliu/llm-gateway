#!/bin/sh
# LLM-Gateway Linux 启动脚本：source conf 注入环境变量 + JAVA_OPTS，exec java
set -e
CONF_FILE="/etc/llmgateway/llmgateway.conf"
[ -f "$CONF_FILE" ] || { echo "配置文件不存在: $CONF_FILE" >&2; exit 1; }
# set -a 使 source conf 期间的变量赋值自动 export（Spring Boot ${ENV} 占位符依赖环境变量）
set -a
. "$CONF_FILE"
set +a
exec /opt/llmgateway/runtime/bin/java $JAVA_OPTS \
  -Dspring.profiles.active=local \
  -jar /opt/llmgateway/bin/llmgateway.jar