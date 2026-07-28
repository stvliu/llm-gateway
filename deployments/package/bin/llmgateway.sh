#!/bin/sh
# ${pkg.name} Linux 启动脚本：source conf 注入环境变量 + JAVA_OPTS，exec java（系统 Java，不内置 JRE）
set -e
# conf 物理在 ${pkg.installFolder}/conf/${pkg.name}.conf，/etc/${pkg.name}/conf 经 ospackage link 指向其
CONF_FILE="/etc/${pkg.name}/conf/${pkg.name}.conf"
[ -f "$CONF_FILE" ] || { echo "配置文件不存在: $CONF_FILE" >&2; exit 1; }
# set -a 使 source conf 期间的变量赋值自动 export（Spring Boot ${ENV} 占位符依赖环境变量）
set -a
. "$CONF_FILE"
set +a
exec java $JAVA_OPTS \
  -Dspring.profiles.active=local \
  -jar ${pkg.installFolder}/bin/${pkg.name}.jar
