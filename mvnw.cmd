@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script
@REM
@REM Optional ENV vars
@REM   JAVA_HOME       - location of a JDK home dir (mandatory if java not in PATH)
@REM   MAVEN_OPTS      - (Optional) Java runtime options used when maven is run.
@REM ----------------------------------------------------------------------------

@setlocal
set ERROR_CODE=0

@REM set project home directory
set "PROJECT_DIR=%~dp0"

@REM set maven wrapper jar path
set "WRAPPER_JAR=%PROJECT_DIR%.mvn\wrapper\maven-wrapper.jar"

@REM check if wrapper jar exists, if not download it
if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper JAR...
    if not exist "%PROJECT_DIR%.mvn\wrapper" mkdir "%PROJECT_DIR%.mvn\wrapper"
    powershell -Command "& {Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%WRAPPER_JAR%'}"
)

@REM Find Java
if not "%JAVA_HOME%" == "" goto gotJavaHome
where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    exit /b 1
)
:gotJavaHome
set "JAVA=%JAVA_HOME%\bin\java"

@REM Run Maven
%JAVA% %MAVEN_OPTS% -Dmaven.multiModuleProjectDirectory="%PROJECT_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
set ERROR_CODE=%ERRORLEVEL%

@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /b %ERROR_CODE%
