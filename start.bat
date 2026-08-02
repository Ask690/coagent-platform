@echo off
chcp 65001 >nul
title CoAgent 一键启动
cd /d %~dp0

echo ============================================================
echo  CoAgent 企业智能客服 · 多智能体协作平台
echo ============================================================

REM ---- 探测 JDK ----
if not defined JAVA_HOME if exist "%USERPROFILE%\.jdks" (
  for /f %%i in ('dir /b /ad "%USERPROFILE%\.jdks" 2^>nul') do set "JAVA_HOME=%USERPROFILE%\.jdks\%%i"
)
if not defined JAVA_HOME (
  echo [错误] 未找到 JDK，请先安装 JDK 17+ 并设置 JAVA_HOME 环境变量。
  pause
  exit /b 1
)
echo [1/3] JDK: %JAVA_HOME%

REM ---- 构建后端 ----
if not exist "backend\target\coagent-backend-1.0.0.jar" (
  echo [2/3] 首次运行，构建后端（约 2~5 分钟，需联网下载依赖）...
  pushd backend
  call mvnw.cmd -q -DskipTests package
  popd
) else (
  echo [2/3] 后端已构建
)

REM ---- 安装前端依赖 ----
if not exist "frontend\node_modules" (
  echo [2/3] 安装前端依赖...
  pushd frontend
  call npm install
  popd
)

echo [3/3] 启动服务...
start "coagent-backend" /min "%JAVA_HOME%\bin\java.exe" -jar "backend\target\coagent-backend-1.0.0.jar"
start "coagent-frontend" /min cmd /c "cd /d %~dp0frontend && npm run dev"

echo.
echo  CoAgent 已启动！请打开浏览器访问 http://localhost:5173
echo  （后端 :8080，前端 :5173；默认 Mock 模式，无需 API Key）
echo.
pause
