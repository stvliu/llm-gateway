#!/bin/sh
# LLM-Gateway Linux 启动脚本：source conf 注入环境变量 + JAVA_OPTS，exec java
set -e
CONF_FILE="/etc/llm-gateway/llmgateway.conf"
[ -f "$CONF_FILE" ] || { echo "配置文件不存在: $CONF_FILE" >&2; exit 1; }
. "$CONF_FILE"
exec /opt/llm-gateway/runtime/bin/java $JAVA_OPTS \
  -Dspring.profiles.active=local \
  -jar /opt/llm-gateway/bin/llm-gateway.jar