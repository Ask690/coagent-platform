#!/usr/bin/env bash
# ============================================================
# CoAgent 一键启动脚本（Git Bash / Linux / macOS 通用）
# 用法： ./start.sh
# 启动后浏览器访问 http://localhost:5173
# ============================================================
set -e
cd "$(dirname "$0")"

# ---------- 自动探测 JDK ----------
if [ -z "$JAVA_HOME" ] && [ -d "$HOME/.jdks" ]; then
  export JAVA_HOME=$(ls -d "$HOME"/.jdks/*/ 2>/dev/null | head -1 | sed 's:/$::')
fi
if [ -z "$JAVA_HOME" ]; then
  export JAVA_HOME="$(dirname "$(dirname "$(command -v java 2>/dev/null)")" 2>/dev/null)"
fi
if [ -z "$JAVA_HOME" ]; then
  echo "❌ 未找到 JDK 17+，请先安装并设置 JAVA_HOME" && exit 1
fi
echo "✅ 使用 JDK: $JAVA_HOME"

# ---------- 构建后端（若尚未打包） ----------
JAR="backend/target/coagent-backend-1.0.0.jar"
if [ ! -f "$JAR" ]; then
  echo "▶ 首次运行，正在构建后端（首次需下载依赖，约 2~5 分钟）..."
  (cd backend && ./mvnw -q -DskipTests package)
fi

# ---------- 前端依赖 ----------
if [ ! -d frontend/node_modules ]; then
  echo "▶ 安装前端依赖..."
  (cd frontend && npm install)
fi

# ---------- 启动 ----------
echo "▶ 启动后端 http://localhost:8080"
(cd backend && "$JAVA_HOME/bin/java" -jar "$(basename "$JAR")") &
BACKEND_PID=$!

echo "▶ 启动前端 http://localhost:5173"
(cd frontend && npm run dev) &
FRONTEND_PID=$!

trap 'echo; echo "🛑 正在停止服务..."; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null' INT TERM
echo
echo "============================================================"
echo " ✅ CoAgent 已启动！浏览器打开 http://localhost:5173"
echo "    · 默认 Mock 模式（无需 API Key）"
echo "    · 接入真实 DeepSeek：export DEEPSEEK_API_KEY=sk-xxx COAGENT_MOCK=false 后重启"
echo "============================================================"
wait
