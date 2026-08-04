# CoAgent 部署镜像：前端(Vue3+Vite) 构建后由 Maven 拷进后端 jar，单容器运行
# 放在仓库【根目录】，与 backend/、frontend/ 同级

# ---------- Stage 1: 构建前端 ----------
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ---------- Stage 2: 构建后端 jar（把上一步的 dist 拷进 jar 的 /static） ----------
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY backend/ ./backend/
COPY --from=frontend-build /app/frontend/dist ./frontend/dist
WORKDIR /app/backend
RUN chmod +x mvnw && ./mvnw -B -q -DskipTests package

# ---------- Stage 3: 运行时（体积小，只含 JRE） ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/target/coagent-backend-1.0.0.jar app.jar

# 注意：故意不加 -Dspring.profiles.active=prod
# 默认 profile 才会保持 COAGENT_MOCK=true（零Key可跑）且用相对路径 H2，避免 prod profile 的坑
#
# Render/多数 PaaS 通过 PORT 环境变量指定监听端口，没有则用 8080
# -Xmx / SerialGC：给免费实例常见的 512MB 内存留余量，降低 OOM 风险
ENV JAVA_OPTS="-Xmx400m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
