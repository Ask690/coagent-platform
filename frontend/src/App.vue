<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, UploadFilled, Document, Delete } from '@element-plus/icons-vue'
import SessionSidebar from './components/SessionSidebar.vue'
import ChatPanel from './components/ChatPanel.vue'
import AgentTimeline from './components/AgentTimeline.vue'
import {
  getSessions, createSession, getMessages,
  getTickets, getDocuments, uploadDocument, deleteDocument,
  chatStream,
} from './api'

// ---------- 会话 ----------
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])

// ---------- 对话状态 ----------
const input = ref('')
const quickInput = ref('') // 顶部搜索框
const sending = ref(false)
const activity = ref([]) // 本轮 Agent 运行时间线

// ---------- 右侧面板 ----------
const rightTab = ref('activity')
const tickets = ref([])
const documents = ref([])

const SUGGESTIONS = [
  '帮我查一下订单 JD2025002 到哪了',
  '七天无理由退货政策是什么？',
  '怎么开发票？',
  '物流太慢了，我要投诉',
]

async function loadSessions() {
  try {
    sessions.value = await getSessions()
  } catch (err) {
    ElMessage.error('加载会话列表失败：' + (err.message || '网络异常'))
  }
}

async function newSession() {
  try {
    const { sessionId } = await createSession()
    currentSessionId.value = sessionId
    messages.value = []
    activity.value = []
    await loadSessions()
  } catch (err) {
    ElMessage.error('新建会话失败：' + (err.message || '网络异常'))
  }
}

async function selectSession(id) {
  if (sending.value) return
  currentSessionId.value = id
  activity.value = []
  try {
    const list = await getMessages(id)
    messages.value = list.map((m) => ({ role: m.role, content: m.content }))
  } catch (err) {
    ElMessage.error('加载消息失败：' + (err.message || '网络异常'))
  }
}

function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  if (!currentSessionId.value) {
    ElMessage.warning('请先新建会话')
    return
  }
  input.value = ''
  messages.value.push({ role: 'USER', content: text })
  const assistant = { role: 'ASSISTANT', content: '' }
  messages.value.push(assistant)
  activity.value = []
  sending.value = true

  chatStream(currentSessionId.value, text, handleEvent, (err) => {
    sending.value = false
    ElMessage.error(err.message || '连接失败，请确认后端已启动')
  })
}

/** 顶部搜索框快捷提问 */
function quickSend() {
  const text = quickInput.value.trim()
  if (!text || sending.value) return
  if (!currentSessionId.value) {
    ElMessage.warning('请先新建会话')
    return
  }
  input.value = text
  quickInput.value = ''
  send()
}

function handleEvent(ev) {
  const { type, data } = ev
  switch (type) {
    case 'session':
      if (!currentSessionId.value) currentSessionId.value = data.sessionId
      break
    case 'agent_start':
      activity.value.push({
        id: activity.value.length + 1,
        name: data.name,
        title: data.title,
        status: 'running',
        route: null,
        tools: [],
      })
      break
    case 'route':
      if (activity.value.length) {
        activity.value[activity.value.length - 1].route = {
          intent: data.intent,
          reason: data.reason,
        }
      }
      break
    case 'tool_call':
      attachTool({ name: data.name, args: data.args, status: 'running', result: null })
      break
    case 'tool_result':
      attachTool({ name: data.name, result: data.result, status: 'done' })
      break
    case 'token':
      if (messages.value.length) {
        messages.value[messages.value.length - 1].content += data.text
      }
      break
    case 'agent_end':
      if (activity.value.length) {
        activity.value[activity.value.length - 1].status = 'done'
      }
      break
    case 'done':
      sending.value = false
      refreshAfterTurn()
      break
    case 'error':
      sending.value = false
      ElMessage.error(data.message || '服务异常')
      break
  }
}

/** 把工具事件挂到当前正在运行的 Agent 卡片上 */
function attachTool(tool) {
  const last = activity.value[activity.value.length - 1]
  if (!last) return
  if (tool.status === 'done') {
    const hit = last.tools.find((t) => t.name === tool.name)
    if (hit) {
      hit.result = tool.result
      hit.status = 'done'
    }
  } else {
    last.tools.push(tool)
  }
}

async function refreshAfterTurn() {
  await loadSessions()
  await loadTickets()
}

// ---------- 右侧数据 ----------
async function loadTickets() {
  tickets.value = await getTickets()
}
async function loadDocuments() {
  documents.value = await getDocuments()
}
async function handleUpload(file) {
  const res = await uploadDocument(file)
  if (res.ok) {
    ElMessage.success('文档上传并入库成功')
  } else {
    const body = await res.json().catch(() => ({}))
    ElMessage.error(body.error || '上传失败')
  }
  await loadDocuments()
  return false // 阻止 el-upload 默认行为
}
async function handleDeleteDoc(id) {
  await deleteDocument(id)
  ElMessage.success('文档已删除')
  await loadDocuments()
}

onMounted(async () => {
  await Promise.all([loadSessions(), loadTickets(), loadDocuments()])
})

// ---------- 展示辅助 ----------
function tagType(status) {
  if (status === 'CLOSED') return 'info'
  if (status === 'PROCESSING') return 'warning'
  return 'danger'
}
function formatTime(iso) {
  if (!iso) return ''
  return iso.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <div class="app">
    <!-- 背景装饰光晕 -->
    <div class="bg-decor bg-decor-1"></div>
    <div class="bg-decor bg-decor-2"></div>

    <!-- ===== 亚马逊式顶部导航 ===== -->
    <header class="amz-topnav">
      <div class="amz-logo" @click="newSession" title="新建会话">
        <span class="logo-mark">🛒</span>
        <span class="logo-text">Co<span class="logo-accent">Agent</span></span>
        <span class="logo-glow"></span>
      </div>

      <div class="top-search">
        <input
          v-model="quickInput"
          class="top-search-input"
          placeholder="搜索：查订单、退货政策、开发票… 回车即问"
          @keyup.enter="quickSend"
        />
        <button class="top-search-btn" @click="quickSend">
          <el-icon><Search /></el-icon>
        </button>
      </div>

      <div class="top-status">
        <span class="live-dot"></span>
        <span class="live-text">智能体在线</span>
        <span class="top-divider"></span>
        <span class="top-chip">DeepSeek · 多智能体</span>
      </div>
    </header>

    <!-- ===== 主体三栏 ===== -->
    <div class="app-body">
      <SessionSidebar
        :sessions="sessions"
        :current-id="currentSessionId"
        @create="newSession"
        @select="selectSession"
      />

      <main class="main">
        <header class="chat-header">
          <div class="title">
            <span v-if="!currentSessionId">欢迎使用 CoAgent 客服助手</span>
            <span v-else>
              {{ sessions.find((s) => s.sessionId === currentSessionId)?.title || '会话' }}
            </span>
          </div>
          <div class="badges">
            <span class="badge">🤖 多智能体协作</span>
            <span class="badge badge-model">Supervisor 编排</span>
          </div>
        </header>

        <ChatPanel
          v-model="input"
          :messages="messages"
          :sending="sending"
          :suggestions="SUGGESTIONS"
          @send="send"
        />
      </main>

      <aside class="right-panel">
        <el-tabs v-model="rightTab" class="right-tabs">
          <el-tab-pane label="运行过程" name="activity">
            <AgentTimeline :activity="activity" :sending="sending" />
          </el-tab-pane>
          <el-tab-pane label="工单中心" name="tickets">
            <div v-if="!tickets.length" class="empty-tip">暂无工单，尝试说「我要投诉」</div>
            <div v-for="t in tickets" :key="t.ticketNo" class="ticket-card animate-in">
              <div class="ticket-head">
                <span class="ticket-no">{{ t.ticketNo }}</span>
                <el-tag size="small" :type="tagType(t.status)">{{ t.status }}</el-tag>
              </div>
              <div class="ticket-type">{{ t.type }}</div>
              <div class="ticket-title">{{ t.title }}</div>
              <div class="ticket-time">创建于 {{ formatTime(t.createdAt) }}</div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="知识库" name="knowledge">
            <el-upload
              class="kb-upload"
              drag
              :show-file-list="false"
              :http-request="(o) => handleUpload(o.file)"
              accept=".txt,.md,.markdown"
            >
              <el-icon class="kb-upload-icon"><UploadFilled /></el-icon>
              <div class="kb-upload-title">上传知识文档</div>
              <div class="kb-hint">支持 .txt / .md，自动切块入库</div>
            </el-upload>
            <div v-if="!documents.length" class="empty-tip">知识库为空，上传文档后可被 RAG 检索</div>
            <div v-for="d in documents" :key="d.id" class="doc-item">
              <el-icon class="doc-icon"><Document /></el-icon>
              <div class="doc-info">
                <div class="doc-name">{{ d.fileName }}</div>
                <div class="doc-meta">{{ d.chunkCount }} 个知识块 · {{ formatTime(d.createdAt) }}</div>
              </div>
              <el-button link type="danger" @click="handleDeleteDoc(d.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.app {
  position: relative;
  height: 100%;
  min-width: 1020px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 背景装饰光晕 ===== */
.bg-decor {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.16;
  pointer-events: none;
  z-index: 0;
  animation: floatY 14s ease-in-out infinite;
}
.bg-decor-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  right: 8%;
  background: radial-gradient(circle, var(--amz-orange), transparent 70%);
}
.bg-decor-2 {
  width: 380px;
  height: 380px;
  bottom: -140px;
  left: 12%;
  background: radial-gradient(circle, #232f3e, transparent 70%);
  animation-delay: 4s;
}

/* ===== 顶部导航（亚马逊深蓝黑） ===== */
.amz-topnav {
  position: relative;
  z-index: 20;
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
  background: linear-gradient(180deg, #1a2530, var(--amz-dark));
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}
.amz-logo {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  transition: all 0.2s;
  overflow: hidden;
}
.amz-logo:hover {
  border-color: rgba(255, 255, 255, 0.35);
}
.logo-mark {
  font-size: 20px;
  filter: drop-shadow(0 0 6px rgba(255, 153, 0, 0.5));
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.logo-accent {
  color: var(--amz-orange);
  text-shadow: 0 0 12px rgba(255, 153, 0, 0.5);
}
.logo-glow {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 60px;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  left: -80px;
  animation: shimmer 3.2s ease-in-out infinite;
}

/* 顶部搜索框（亚马逊式） */
.top-search {
  flex: 1;
  max-width: 560px;
  display: flex;
  height: 38px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: box-shadow 0.25s;
}
.top-search:focus-within {
  box-shadow: 0 0 0 3px var(--amz-orange);
}
.top-search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 0 14px;
  font-size: 14px;
  color: var(--amz-text);
  background: transparent;
}
.top-search-btn {
  width: 46px;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #fff;
  background: linear-gradient(135deg, var(--amz-orange), #ffb643);
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.top-search-btn:hover {
  background: linear-gradient(135deg, var(--amz-orange-dark), var(--amz-orange));
  transform: scale(1.05);
}

/* 顶部状态 */
.top-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #d5d9d9;
}
.live-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #48d98a;
  box-shadow: 0 0 0 0 rgba(72, 217, 138, 0.6);
  animation: pulseGlow 2s infinite;
}
.top-divider {
  width: 1px;
  height: 18px;
  background: rgba(255, 255, 255, 0.2);
}
.top-chip {
  padding: 3px 10px;
  border: 1px solid var(--amz-orange);
  color: var(--amz-yellow);
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.5px;
}

/* ===== 主体三栏 ===== */
.app-body {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  min-height: 0;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-left: 1px solid var(--amz-border);
  border-right: 1px solid var(--amz-border);
  background: var(--amz-bg);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 24px;
  background: var(--amz-card);
  border-bottom: 1px solid var(--amz-border);
}
.chat-header .title {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 420px;
}
.badges {
  display: flex;
  gap: 8px;
}
.badge {
  font-size: 12px;
  color: var(--amz-sub);
  background: var(--amz-bg);
  border: 1px solid var(--amz-border);
  padding: 3px 10px;
  border-radius: 999px;
}
.badge-model {
  color: #fff;
  background: linear-gradient(135deg, #1a2530, var(--amz-dark-2));
  border-color: transparent;
}

/* ===== 右侧面板 ===== */
.right-panel {
  width: 344px;
  background: var(--amz-card);
  padding: 0 4px;
  overflow-y: auto;
}
.right-tabs {
  height: 100%;
}
.right-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.right-tabs :deep(.el-tabs__item) {
  font-weight: 500;
  color: var(--amz-sub);
}
.right-tabs :deep(.el-tabs__item.is-active) {
  color: var(--amz-dark);
  font-weight: 700;
}
.right-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, var(--amz-orange), var(--amz-orange-dark));
  height: 3px;
  border-radius: 2px;
}
.right-tabs :deep(.el-tabs__content) {
  padding: 4px 14px 16px;
}

.empty-tip {
  color: var(--amz-sub);
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
  line-height: 1.8;
}

/* 工单卡片（亚马逊商品卡风格） */
.ticket-card {
  border: 1px solid var(--amz-border);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;
  background: var(--amz-card);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.22s;
  cursor: default;
}
.ticket-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--amz-orange);
}
.ticket-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.ticket-no {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 13px;
  color: var(--amz-price);
  font-weight: 700;
}
.ticket-type {
  font-size: 12px;
  color: var(--amz-warn);
  margin-bottom: 4px;
  font-weight: 600;
}
.ticket-title {
  font-size: 13px;
  color: var(--amz-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ticket-time {
  font-size: 12px;
  color: var(--amz-sub);
  margin-top: 6px;
}

/* 知识库 */
.kb-upload {
  margin-bottom: 12px;
}
.kb-upload :deep(.el-upload-dragger) {
  background: linear-gradient(180deg, #fdf8ee, #fff);
  border: 1.5px dashed #f3c878;
  border-radius: 8px;
  transition: all 0.25s;
}
.kb-upload :deep(.el-upload-dragger:hover) {
  border-color: var(--amz-orange);
  background: #fffaf0;
  box-shadow: 0 0 0 4px rgba(255, 153, 0, 0.12);
  transform: translateY(-1px);
}
.kb-upload-icon {
  font-size: 36px;
  color: var(--amz-orange);
  filter: drop-shadow(0 2px 4px rgba(255, 153, 0, 0.3));
}
.kb-upload-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--amz-dark);
  margin-top: 6px;
}
.kb-hint {
  font-size: 12px;
  color: var(--amz-sub);
  margin-top: 4px;
}
.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--amz-border);
  transition: background 0.2s;
}
.doc-item:hover {
  background: #faf9f7;
}
.doc-icon {
  color: var(--amz-orange);
  font-size: 18px;
}
.doc-info {
  flex: 1;
  min-width: 0;
}
.doc-name {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-meta {
  font-size: 12px;
  color: var(--amz-sub);
}
</style>
