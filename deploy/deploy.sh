#!/usr/bin/env bash
# ============================================================
# CoAgent 服务器一键部署脚本（CentOS/Ubuntu）
# 在云服务器上执行： bash deploy.sh
#
# 前提（一次性）：
#   1. 已安装 Git、JDK 21+、Node.js 18+（脚本会自动检查）
#   2. 代码已在服务器（git clone 或上传）
#   3. 设置环境变量 DEEPSEEK_API_KEY
#
# 部署产物：
#   /opt/coagent/app/coagent-backend-1.0.0.jar   后端可执行 jar
#   /opt/coagent/static/                         前端静态文件
#   /opt/coagent/data/                           数据库文件
# ============================================================
set -e

APP_DIR="/opt/coagent"
SOURCE_DIR="$(cd "$(dirname "$0")/.." && pwd)"   # 仓库根目录（deploy/ 的上一级）

echo "=============================================="
echo " CoAgent 生产部署"
echo " 源码目录: $SOURCE_DIR"
echo "=============================================="

# ---------- 0. 前置检查 ----------
command -v java >/dev/null || { echo "❌ 缺少 JDK，请先安装（yum install java-21-openjdk 或 apt install openjdk-21-jdk）"; exit 1; }
command -v node >/dev/null || { echo "❌ 缺少 Node.js，请先安装 18+"; exit 1; }
command -v mvn >/dev/null || { echo "⚠ 未检测到 Maven，将使用项目内置 mvnw"; }

if [ -z "$DEEPSEEK_API_KEY" ]; then
  echo "⚠ 未设置 DEEPSEEK_API_KEY，将使用 Mock 模式。"
  echo "  生产请： export DEEPSEEK_API_KEY=sk-xxx 后重跑"
fi

mkdir -p "$APP_DIR"/{app,static,data,logs}

# ---------- 1. 构建前端 ----------
echo "▶ [1/4] 构建前端..."
cd "$SOURCE_DIR/frontend"
npm install --silent
npm run build
rm -rf "$APP_DIR/static"/*
cp -r dist/* "$APP_DIR/static/"
echo "  前端产物已复制到 $APP_DIR/static/"

# ---------- 2. 构建后端 ----------
echo "▶ [2/4] 构建后端..."
cd "$SOURCE_DIR/backend"
./mvnw -q -DskipTests package
cp target/coagent-backend-1.0.0.jar "$APP_DIR/app/"

# ---------- 3. 写启动脚本 ----------
echo "▶ [3/4] 生成启动脚本..."
cat > "$APP_DIR/app/run.sh" <<EOF
#!/usr/bin/env bash
export JAVA_HOME="\${JAVA_HOME:-\$(dirname \$(dirname \$(command -v java)))}"
export DEEPSEEK_API_KEY="\${DEEPSEEK_API_KEY}"
export COAGENT_MOCK="\${COAGENT_MOCK:-false}"
export AI_MODEL="\${AI_MODEL:-deepseek-chat}"
exec "\$JAVA_HOME/bin/java" -jar -Dspring.profiles.active=prod "$APP_DIR/app/coagent-backend-1.0.0.jar" >> "$APP_DIR/logs/app.log" 2>&1
EOF
chmod +x "$APP_DIR/app/run.sh"

# ---------- 4. 注册 systemd 服务（可选） ----------
echo "▶ [4/4] 注册 systemd 服务..."
cat > /etc/systemd/system/coagent.service <<'EOF'
[Unit]
Description=CoAgent Multi-Agent Customer Service
After=network.target

[Service]
Type=simple
User=root
EnvironmentFile=/etc/coagent.env
ExecStart=/opt/coagent/app/run.sh
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

if [ -f /etc/coagent.env ]; then
  echo "  /etc/coagent.env 已存在，跳过（如需更新请手动编辑）"
else
  cat > /etc/coagent.env <<'EOF'
DEEPSEEK_API_KEY=sk-在这里填入你的Key
COAGENT_MOCK=false
AI_MODEL=deepseek-chat
RAG_MODE=lexical
EOF
  echo "  已生成 /etc/coagent.env（请编辑填入真实 Key）"
fi

systemctl daemon-reload
systemctl enable coagent.service
echo
echo "=============================================="
echo " 部署完成！"
echo " 编辑 Key：    vi /etc/coagent.env"
echo " 启动服务：    systemctl start coagent"
echo " 查看日志：    tail -f /opt/coagent/logs/app.log"
echo " 访问地址：    http://<服务器IP>:8080"
echo "=============================================="
