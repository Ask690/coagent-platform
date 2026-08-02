#!/usr/bin/env bash
# ============================================================
# CoAgent 最简部署脚本（CentOS/Ubuntu）
#
# 核心思路：前端已打进后端 jar（backend/pom.xml 打包时自动复制 dist），
# 部署 = 上传 1 个 jar + JDK 21 + 1 条启动命令。
#
# 服务器前置条件（一次性）：
#   1. JDK 21+：yum install java-21-openjdk 或 apt install openjdk-21-jdk
#   2. 无需 Node / 无需 Maven（jar 在本地已构建好）
#
# 用法：
#   本机构建好 jar 后，把仓库上传到服务器（git clone 或 scp），然后：
#     bash deploy/deploy.sh
#   或只上传 jar，用附录中的单条命令直接跑。
#
# 部署产物：
#   /opt/coagent/app/coagent-backend-1.0.0.jar   后端可执行 jar（含前端页面）
#   /opt/coagent/data/                           数据库文件
#   /opt/coagent/logs/app.log                    运行日志
# ============================================================
set -e

APP_DIR="/opt/coagent"
SOURCE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_NAME="coagent-backend-1.0.0.jar"

echo "=============================================="
echo " CoAgent 最简部署（单 jar 模式）"
echo " 源码目录: $SOURCE_DIR"
echo "=============================================="

# ---------- 0. 前置检查 ----------
command -v java >/dev/null || { echo "❌ 缺少 JDK 21，请先安装（yum install java-21-openjdk 或 apt install openjdk-21-jdk）"; exit 1; }

# ---------- 1. 定位 jar：优先用已构建产物 ----------
JAR_SRC=""
if [ -f "$SOURCE_DIR/backend/target/$JAR_NAME" ]; then
  JAR_SRC="$SOURCE_DIR/backend/target/$JAR_NAME"
  echo "▶ 使用已构建 jar：$JAR_SRC"
elif [ -f "$SOURCE_DIR/$JAR_NAME" ]; then
  JAR_SRC="$SOURCE_DIR/$JAR_NAME"
  echo "▶ 使用仓库根目录 jar：$JAR_SRC"
else
  echo "▶ 未找到构建产物，将在服务器上构建（需要 Maven + Node，较慢）..."
  if ! command -v mvn >/dev/null; then echo "❌ 服务器无 Maven，请先在本机构建 jar 再部署"; exit 1; fi
  if ! command -v node >/dev/null; then echo "❌ 服务器无 Node.js，无法构建前端，请先在本机构建 jar"; exit 1; fi
  (cd "$SOURCE_DIR/frontend" && npm install --silent && npm run build)
  (cd "$SOURCE_DIR/backend" && ./mvnw -q -DskipTests package)
  JAR_SRC="$SOURCE_DIR/backend/target/$JAR_NAME"
fi

mkdir -p "$APP_DIR"/{app,data,logs}

# ---------- 2. 拷贝 jar ----------
echo "▶ 部署 jar → $APP_DIR/app/"
cp "$JAR_SRC" "$APP_DIR/app/$JAR_NAME"

# ---------- 3. 生成启动脚本 ----------
cat > "$APP_DIR/app/run.sh" <<EOF
#!/usr/bin/env bash
export DEEPSEEK_API_KEY="\${DEEPSEEK_API_KEY}"
export COAGENT_MOCK="\${COAGENT_MOCK:-false}"
export AI_MODEL="\${AI_MODEL:-deepseek-chat}"
export JAVA_HOME="\${JAVA_HOME:-\$(dirname \$(dirname \$(readlink -f \$(command -v java) 2>/dev/null || command -v java)))}"
exec "\${JAVA_HOME:-/usr}/bin/java" -Dspring.profiles.active=prod -jar "$APP_DIR/app/$JAR_NAME" >> "$APP_DIR/logs/app.log" 2>&1
EOF
chmod +x "$APP_DIR/app/run.sh"

# ---------- 4. 注册 systemd 服务 ----------
cat > /etc/systemd/system/coagent.service <<EOF
[Unit]
Description=CoAgent Multi-Agent Customer Service
After=network.target

[Service]
Type=simple
User=root
EnvironmentFile=/etc/coagent.env
ExecStart=$APP_DIR/app/run.sh
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
systemctl enable coagent.service >/dev/null 2>&1 || true

echo
echo "=============================================="
echo " 部署完成！"
echo " 编辑 Key：    vi /etc/coagent.env"
echo " 启动服务：    systemctl start coagent"
echo " 查看日志：    tail -f /opt/coagent/logs/app.log"
echo " 访问地址：    http://<服务器IP>:8080"
echo ""
echo " 不想用 systemd？最简一条命令："
echo "   DEEPSEEK_API_KEY=sk-xxx java -Dspring.profiles.active=prod -jar /opt/coagent/app/$JAR_NAME"
echo "=============================================="
