# CoAgent 🛒 多智能体协作客服平台

> 基于 **Java 21 + Spring Boot 3.5 + Spring AI 1.0 + DeepSeek + Vue3** 的多智能体客服平台。
> **编排者（Supervisor）调度专精 Agent 协作**，SSE 流式实时可见每一步；**开箱即跑，零 API Key 也能完整体验**。

![GitHub stars](https://img.shields.io/github/stars/Ask690/coagent-platform?style=social&label=Stars)
![MIT License](https://img.shields.io/badge/License-MIT-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.9-6db33f)
![Vue 3](https://img.shields.io/badge/Vue-3-42b883)

---

## ✨ 在线体验

👉 **公网 Demo**：`https://jimmy-reports-significance-regards.trycloudflare.com`

> 这是本机的临时穿透隧道，重启后地址会变；按下方「快速开始」自己跑一遍，10 分钟即可拥有同样可公网访问的地址（免费）。

**试试这几句话，直观感受多智能体协作：**

| 你问 | 会发生什么 |
|---|---|
| 我订单 JD2025001 能退货吗 | 🔗 **多步链路**：查订单 → 检索退货政策 → 综合答复 |
| 七天无理由退货政策是什么 | 📚 **知识库 Agent**：RAG 检索 + 引用来源 |
| 帮我查订单 JD2025002 到哪了 | 🛠 **业务查询 Agent**：调用工具查物流 |
| 我要投诉！物流太慢 | 🎫 **工单 Agent**：自动建工单落库 |
| 你好呀 | 💬 直接友好回复 |

---

## 🚀 快速开始

> 环境：JDK 17+（推荐 21）、Node 18+。Maven 用项目内置 wrapper。

```bash
# 一键启动（Git Bash / macOS / Linux）
./start.sh
# 或手动：
#   cd backend && ./mvnw spring-boot:run      # 后端 :8080
#   cd frontend && npm install && npm run dev # 前端 :5173
```

浏览器打开 **http://localhost:5173** —— 默认 **Mock 模式，无需任何 API Key** 即可体验全部功能。

### 🔑 接入真实 DeepSeek（两步，代码零改动）

```bash
export DEEPSEEK_API_KEY=sk-你的Key
export COAGENT_MOCK=false   # 关闭 Mock，切换到真实大模型
```

---

## 🧠 架构一览

```
用户 ──▶ 编排者(Supervisor)
          ├─ 双路由：LlmRouter 结构化路由 + KeywordRouter 关键词兜底
          ├─ 📚 知识库 Agent   → RAG 检索（BM25 词法 / 向量余弦 双实现）
          ├─ 🛠 业务查询 Agent → 工具调用（订单 / 物流）
          ├─ 🎫 工单 Agent     → 工单创建落库
          └─ 🔗 多步链路 CHAIN → 查订单 → 检索政策 → 汇总作答
                    │
                    ▼
         SSE 流式实时可视化（路由决策 / 工具调用 / 流式回复）
```

每一轮对话，前端都会**实时展示**编排者如何分派、专精 Agent 做了什么、调用了哪些工具、结果如何——全程可见，非黑盒。

---

## 🛠 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Java 21 · Spring Boot 3.5 · Spring AI 1.0 |
| 大模型 | DeepSeek（OpenAI 兼容，可换任意兼容模型） |
| 降级 | MockChatModel（零 Key 完整演示） |
| 数据库 | H2 默认 / MySQL 可选（Spring Data JPA） |
| RAG | BM25 词法 + 向量余弦 双实现，一键切换 |
| 前端 | Vue3 + Vite + Element Plus |
| 部署 | 单 jar 全打包 / systemd / 内网穿透 |

---

## ☁️ 部署

**单 jar 全打包** —— Maven 打包时自动把前端页面打进 jar，部署只需 1 个文件：

```bash
cd backend && ./mvnw -DskipTests package
# 产物：target/coagent-backend-1.0.0.jar（含全部页面，约 65MB）

# 服务器 / 任意有 JDK 21 的机器：
DEEPSEEK_API_KEY=sk-xxx java -Dspring.profiles.active=prod -jar coagent-backend-1.0.0.jar
```

**免费上线（无服务器）**：用 cloudflared 内网穿透即可获得公网 HTTPS 地址，详见 `deploy/` 目录。

---

## 💡 设计思路

- **为什么多智能体，而不是一个大模型？**
  单一 Prompt 承担"意图识别 + 检索 + 工具 + 安抚"会导致指令冲突、上下文膨胀。拆分为职责单一的 Agent 后，编排层可观测、可灰度、可替换，符合"低耦合高内聚"。
- **为什么工具调用由编排层驱动？**
  由编排层驱动保证**确定性、可观测、可测试**；同一批 `@Tool` 方法可随时切换为模型自主调用，灵活性与可控性兼顾。
- **为什么先 BM25 再上向量检索？**
  零外部依赖、离线可跑。`Retriever` 是接口，生产替换只需新增一个实现类——先跑通再演进。
- **Mock 与真实模型如何无缝切换？**
  都实现 Spring AI `ChatModel` 接口，`COAGENT_MOCK` 一个开关切换，业务代码零改动。

---

## 📁 项目结构

```
backend/src/main/java/com/coagent/
├── agent/          # 编排核心：Orchestrator / 路由 / 链路编排
├── rag/            # 检索：BM25 + 向量 双实现 + Mock Embedding
├── agent/tools/    # 订单 / 政策 / 工单 工具
├── support/        # MockChatModel 降级模型
└── controller/     # SSE 流式 + REST 端点
frontend/src/
├── components/     # 聊天面板 / Agent 时间线 / 会话侧栏
└── api/            # SSE 流式客户端
```

---

## 🤝 一起完善

欢迎提交 Issue / PR，一起把 Agent 架构做得更好。对多智能体编排感兴趣的朋友，欢迎在 GitHub 交流。

## ⭐ 支持

如果这个项目对你有帮助，点个 ⭐ **Star** 就是最大的支持！

## 📄 License

[MIT](LICENSE)
