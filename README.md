# CoAgent · 企业智能客服多智能体协作平台

> 一个基于 **Java 21 + Spring Boot 3.5 + Spring AI 1.0 + DeepSeek + Vue3** 的**多智能体（Multi-Agent）**企业级客服协作平台，可离线 Mock 完整运行，接入真实大模型仅需一个环境变量。

## 🎯 项目定位（面试叙事主线）

客服降本增效是 2025–2026 企业 AI 落地的**第一优先级场景**。本项目把传统的单模型客服升级为 **Supervisor 编排的多智能体协作系统**：

- **编排者（Supervisor）** 统一做意图识别与任务调度，`LLM 结构化路由 + 关键词规则兜底`双保险；
- **知识库 Agent**：RAG 检索（BM25 词法检索，可平滑替换向量检索）后生成式作答；
- **业务查询 Agent**：识别订单号 → 调用**工具**（Function Calling）查询订单/物流 → 基于工具结果回答；
- **工单 Agent**：识别投诉/售后意图 → 自动**创建工单**落库 → 安抚回复；
- 前端 **SSE 流式**渲染，Agent 运行过程、工具调用全过程**实时可视化**。

---

## ✨ 已实现的演示效果

| 用户问题 | 路由意图 | 展示能力 |
|---|---|---|
| 帮我查订单 JD2025002 到哪了 | BUSINESS | 工具调用 + 流式回复 |
| 七天无理由退货政策是什么 | KNOWLEDGE | RAG 检索 + 基于上下文作答 |
| 我订单 JD2025001 能退货吗 | **CHAIN** | **多步链路**：查订单 → 检索政策 → 综合分析 |
| 我要投诉！物流太慢 | TICKET | 自动创建工单并落库 |
| 你好呀 | DIRECT | 直接友好回复 |

---

## 🧱 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 后端 | Java 21 · Spring Boot 3.5 · Spring AI 1.0 | 主流企业级 Java 技术栈 |
| 大模型 | DeepSeek（OpenAI 兼容接口） | 改 `base-url` + `model` 可切任意 OpenAI 兼容模型 |
| 内置降级 | MockChatModel（实现 Spring AI `ChatModel` 接口） | 零 Key 完整演示 |
| 数据库 | H2（默认，零依赖）/ MySQL（profile） | Spring Data JPA |
| RAG 检索 | BM25 词法检索（自实现，`Retriever` 抽象） | 可替换向量检索 |
| 前端 | Vue3 + Vite + Element Plus | SSE 流式 + Agent 可视化 |
| 构建 | Maven Wrapper（自包含） | 无需预装 Maven |

---

## 🏗️ 架构设计

```
┌────────────────────────────── Vue3 前端 (5173) ──────────────────────────────┐
│  会话列表 │ 聊天界面(SSE流式) │ 运行过程可视化 │ 工单中心 │ 知识库上传         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                      │ POST /api/chat/stream (SSE 事件流)
┌──────────────────────────────────────▼──────────────────────────────────────┐
│                          Spring Boot 后端 (8080)                            │
│                                                                            │
│   AgentOrchestrator（多智能体编排器，Supervisor Pattern）                    │
│   ┌──────────────────────────────────────────────────────────────────────┐ │
│   │  编排者 Router（意图路由）                                            │ │
│   │   ├─ LlmRouter      —— DeepSeek 结构化输出 JSON（mock=false 启用）     │ │
│   │   └─ KeywordRouter  —— 关键词规则兜底（mock=true 启用，离线可用）      │ │
│   └──────┬──────────────────┬────────────────────────────────────────────┘ │
│          ▼                  ▼  CHAIN(组合场景)                             │
│   单Agent 分派         AgentChainService（多步链路编排）                    │
│   ┌────────────┐       ┌──────────────────────────────────────────────┐  │
│   │知识库Agent  │       │ Step1 业务查询Agent → 订单状态(工具)            │  │
│   │业务查询Agent│       │ Step2 知识库Agent   → 退货政策(RAG)            │  │
│   │工单Agent    │       │ Step3 汇总Agent     → 综合分析(生成)           │  │
│   └────────────┘       └──────────────────────────────────────────────┘  │
│         │                                │                                │
│         ▼                                ▼                                │
│   Retriever(可切换)               OrderTool / TicketTool                   │
│   ├─ LexicalRetriever(BM25)        @Tool 注解                             │
│   └─ VectorRetriever(余弦相似度)   工单号生成(日期+自增ID)                  │
│       └─ MockEmbeddingModel(离线)                                          │
│                                                                            │
│   H2/MySQL（会话 / 消息 / 工单 / 知识文档）                                 │
└────────────────────────────────────────────────────────────────────────────┘
```

**每轮对话事件流（SSE）：**
```
session → agent_start(编排者) → route(意图/原因) → agent_end(编排者)
       → agent_start(专精Agent) → tool_call/tool_result → token(流式文本)
       → agent_end(专精Agent) → done
```

---

## 🚀 快速启动

> 环境要求：JDK 17+（推荐 21）、Node.js 18+。Maven 由 Wrapper 自动下载。

### 方式一：一键启动（推荐）

```bash
./start.sh        # Git Bash / macOS / Linux
# Windows 建议使用 Git Bash 执行上述脚本
```

浏览器打开 **http://localhost:5173**，默认 **Mock 模式**无需 API Key 即可体验全部功能。

### 方式二：手动启动

```bash
# 1. 后端（默认 Mock 模式，零配置）
cd backend
./mvnw spring-boot:run

# 2. 前端（新终端）
cd frontend
npm install
npm run dev
```

---

## 🔑 接入真实 DeepSeek（两步）

```bash
# 1. 申请 Key：https://platform.deepseek.com
export DEEPSEEK_API_KEY=sk-xxxxxxxx

# 2. 关闭 Mock 并重启后端
export COAGENT_MOCK=false
# 可选：换模型（DeepSeek-V4 等）与自定义地址
export AI_MODEL=deepseek-chat
export AI_BASE_URL=https://api.deepseek.com
```

切换后**代码零改动**：MockChatModel 与 DeepSeek 都实现了同一个 Spring AI `ChatModel` 接口，走同一套 ChatClient 流式管线。

### 环境变量一览

| 变量 | 默认值 | 说明 |
|---|---|---|
| `COAGENT_MOCK` | `true` | `false` 时接入真实大模型 |
| `DEEPSEEK_API_KEY` | `sk-noop` | 真实模型 API Key |
| `AI_MODEL` | `deepseek-chat` | 模型名，可换 `deepseek-reasoner` 等 |
| `AI_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容接口地址 |
| `RAG_MODE` | `lexical` | 检索方式：`lexical`(BM25) / `vector`(余弦相似度，离线 MockEmbedding) |
| `AI_EMBEDDING_PROVIDER` | `mock` | embedding 提供方：`mock`(离线) / `openai`(真实兼容服务) |

### 切换 MySQL

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```
（需在 `application-mysql.yml` 中配置连接信息，仓库已含模板）

---

## ☁️ 生产部署（单 jar 全打包 · 最简）

**核心设计**：Maven 打包时自动把前端 `dist` 复制进 jar 的 `/static`（见 `backend/pom.xml` 的 `copy-frontend-dist`）。因此生产部署**只需 1 个 jar 文件**，服务器无需 Node、无需 Maven、无需单独托管静态文件。

### 方式 A：单命令启动（最快）

```bash
# ① 本机构建（前端自动打进 jar）
cd backend && ./mvnw -DskipTests package
#   产物：backend/target/coagent-backend-1.0.0.jar（约 65MB，含全部页面）

# ② 上传到服务器（服务器只需装 JDK 21）
scp backend/target/coagent-backend-1.0.0.jar root@服务器IP:/opt/coagent/

# ③ 服务器上启动
DEEPSEEK_API_KEY=sk-你的Key \
java -Dspring.profiles.active=prod -jar /opt/coagent/coagent-backend-1.0.0.jar
```

访问 `http://服务器IP:8080`。

### 方式 B：systemd 守护（自动重启 + 开机自启）

```bash
# 服务器上：上传整个仓库后执行
export DEEPSEEK_API_KEY=sk-你的Key
bash deploy/deploy.sh        # 自动拷贝 jar + 注册 systemd 服务
systemctl start coagent
tail -f /opt/coagent/logs/app.log
```

### 生产环境变量（application-prod.yml）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DEEPSEEK_API_KEY` | `sk-noop` | 真实 Key（不设则 Mock 降级，可完整演示） |
| `COAGENT_MOCK` | `false`(prod) | `true` 强制 Mock |
| `AI_MODEL` | `deepseek-chat` | 模型名 |
| `AI_BASE_URL` | `https://api.deepseek.com` | API 地址 |
| `RAG_MODE` | `lexical` | `vector` 切向量检索 |
| `COAGENT_STATIC_DIR` | jar 内置 | 自定义静态目录（可选） |

> 生产默认 DB：H2 文件库 `/opt/coagent/data/coagent`，零依赖可跑；接 MySQL 见 `application-mysql.yml`。
> 生产已关闭 H2 控制台，日志落 `/opt/coagent/logs/app.log`。

---

## 💡 项目亮点（面试讲稿要点）

1. **真正的多智能体协作**：Supervisor 统一编排 + 专精 Agent + 多步链路（Chain），各 Agent 职责单一、提示词独立，符合 Agent 工程化的最佳实践。
2. **双路由降级设计**：`LlmRouter`（大模型结构化路由）+ `KeywordRouter`（关键词兜底）。模型不可用时系统仍可用——这是企业系统的关键工程素质。
3. **可观测性**：工具调用由编排层驱动，全程通过 SSE 事件流暴露到前端（`tool_call → tool_result`），非黑盒。
4. **多步链路编排**：当单个 Agent 无法回答（"我的订单能退吗"既需订单数据又需退货政策），编排器自动走「查订单 → 检索政策 → 综合分析」链路，每步独立可观测。
5. **RAG 双检索可切换**：`Retriever` 抽象接口下实现 BM25 词法检索与向量检索（余弦相似度）两套，经 `RAG_MODE` 一键切换，向量检索离线用 MockEmbedding 也可跑。
5. **SSE 流式**：`Flux<ServerSentEvent>` + 前端 fetch ReadableStream 逐事件解析，打字机式体验。
6. **数据落地**：会话/消息/工单/知识文档全部持久化（H2/MySQL 双支持），服务重启数据不丢。
7. **可测试、可降级**：Mock 模式输出确定性内容，接口稳定，可写集成测试。

### 面试可能被追问 & 参考回答

- **为什么用多智能体而不是一个大模型？**
  单一 Prompt 承担"意图识别 + 检索 + 工具 + 安抚"会导致指令冲突、上下文膨胀、难以定位问题。拆分后每个 Agent 的职责和提示词可控，编排层可观测、可灰度、可替换，符合"低耦合高内聚"的工程原则。

- **工具调用为什么由编排层驱动，而不是模型自主调用？**
  两种都行。模型自主调用（`ChatClient.defaultTools` + Function Calling）更灵活但对输出格式和异常缺少控制；演示场景下由编排层驱动保证**确定性、可观测、可测试**，同一批 `@Tool` 方法可随时切换为模型自主调用，是灵活性与可控性的权衡。

- **检索为什么先用 BM25？**
  零外部依赖、离线可跑、演示稳定。`Retriever` 是接口，生产替换向量检索只需新增一个实现类——先跑通再演进，避免一开始就引入 embedding 模型和向量库的复杂度。
- **多步链路和单 Agent 有什么区别？**
  单 Agent 用一个 Prompt 承担全部任务，指令会冲突、上下文会膨胀。链路把「取数」和「推理」拆成独立步骤：Step1 拿事实（订单状态），Step2 拿规则（退货政策），Step3 才让汇总 Agent 做判断。每步结果结构化传递、可单独测试，这是 Agent 工程里「工具调用 + 多步推理」的经典范式。
- **向量检索离线怎么跑？**
  用 MockEmbeddingModel（本地词袋 hash 伪向量）跑通「向量化 → 余弦排序」整条链路；接真实 Embedding 服务只需 `AI_EMBEDDING_PROVIDER=openai` + 配置 api-key，Mock 自动失效。

- **如何保证多轮对话的记忆？**
  每轮对话持久化到 `chat_message` 表，Agent 生成时取最近 N 条历史拼入上下文；生产可替换为 `MessageChatMemoryAdvisor` 或外部记忆。

- **Mock 模式与真实模型如何无缝切换？**
  `MockChatModel` 实现标准 `ChatModel` 接口并标 `@Primary`，`coagent.ai.mock=false` 时该 Bean 不装配，自动使用 Spring AI 配置的 OpenAiChatModel（指向 DeepSeek），业务代码零改动。

---

## 📁 项目结构

```
coagent-platform/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Boot 3.5.15 + Spring AI 1.0.9
│   ├── mvnw / mvnw.cmd               # Maven Wrapper（自包含）
│   └── src/main/
│       ├── java/com/coagent/
│       │   ├── agent/                # 多智能体核心
│       │   │   ├── AgentOrchestrator.java   # 编排器（Supervisor Pattern）
│       │   │   ├── Router/LlmRouter/KeywordRouter.java  # 路由策略
│       │   │   ├── KnowledgeAgent/BusinessAgent/TicketAgent.java
│       │   │   ├── ChatEvent.java           # SSE 事件协议
│       │   │   └── tools/            # @Tool 工具（订单/工单）
│       │   ├── rag/                  # RAG：Retriever/BM25/切块/入库
│       │   ├── controller/           # SSE 对话 / 会话 / 工单 / 文档
│       │   ├── domain/ repository/   # JPA 实体与仓库
│       │   └── support/MockChatModel.java  # 离线降级模型
│       └── resources/
│           ├── application.yml       # DeepSeek + H2 + 多级配置
│           └── knowledge/            # 内置种子知识文档（5篇）
└── frontend/                         # Vue3 + Element Plus 前端
    └── src/
        ├── api/index.js              # SSE 流式客户端
        ├── App.vue                   # 三栏布局 + 对话状态机
        └── components/               # 会话侧栏/聊天面板/Agent时间线
```

---

## 🗺️ 扩展路线（可讲给面试官听）

- [ ] **向量检索**：接入本地 ONNX 或云 Embedding，`Retriever` 新增 `VectorRetriever` 实现
- [ ] **多 Agent 并行/链路**：编排器支持"检索 → 业务 → 汇总"多步链路
- [ ] **鉴权与限流**：Spring Security + 接口限流 + 敏感词过滤
- [ ] **可观测性**：集成 Micrometer/Trace 与 Agent 耗时埋点
- [ ] **模型无关**：统一 `ChatModel` 抽象，可切换 DeepSeek / GLM / Qwen / OpenAI

---

## ⚖️ 声明

- 业务数据（订单、工单）为演示用 Mock 数据，知识文档为虚构内容，仅用于功能演示。
- 本项目为面试作品，架构设计遵循企业级最佳实践。
