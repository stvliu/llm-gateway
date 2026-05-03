#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script
#
# Environment Variable Prerequisites
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#   MAVEN_OPTS      (Optional) Java runtime options used when Maven is run.
# ----------------------------------------------------------------------------

# Detect OS
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# Resolve the mvnw script path
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
PRGDIR=`dirname "$PRG"`
PROJECT_DIR=`cd "$PRGDIR" >/dev/null; pwd`

# Maven Wrapper jar location
MAVEN_PROJECTBASEDIR="$PROJECT_DIR"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR org.apache.maven.wrapper.MavenWrapperMain"

# Download the wrapper jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"
  WRAPPER_URL=""
  if [ -f "$WRAPPER_PROPERTIES" ]; then
    WRAPPER_URL=`grep "wrapperUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2`
  fi
  if [ -z "$WRAPPER_URL" ]; then
    WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
  fi
  echo "Downloading Maven Wrapper JAR..."
  mkdir -p "$MAVEN_PROJECTBASEDIR/.mvn/wrapper"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "Error: Cannot download maven-wrapper.jar. Please install curl or wget."
    exit 1
  fi
fi

# Find Java
if [ -n "$JAVA_HOME" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi

# Cygwin support
if $cygwin; then
  [ -n "$MAVEN_PROJECTBASEDIR" ] && MAVEN_PROJECTBASEDIR=`cygpath --unix "$MAVEN_PROJECTBASEDIR"`
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Maven executable
exec "$JAVA" $MAVEN_OPTS $WRAPPER_LAUNCHER "$@"
