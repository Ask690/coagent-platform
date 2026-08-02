<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
  sessions.value = await getSessions()
}

async function newSession() {
  const { sessionId } = await createSession()
  currentSessionId.value = sessionId
  messages.value = []
  activity.value = []
  await loadSessions()
}

async function selectSession(id) {
  if (sending.value) return
  currentSessionId.value = id
  activity.value = []
  const list = await getMessages(id)
  messages.value = list.map((m) => ({ role: m.role, content: m.content }))
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
    <SessionSidebar
      :sessions="sessions"
      :current-id="currentSessionId"
      @create="newSession"
      @select="selectSession"
    />

    <main class="main">
      <header class="chat-header">
        <div class="title">
          <span v-if="!currentSessionId">欢迎使用 CoAgent</span>
          <span v-else>
            {{ sessions.find((s) => s.sessionId === currentSessionId)?.title || '会话' }}
          </span>
        </div>
        <div class="badges">
          <span class="badge">🤖 多智能体协作</span>
          <span class="badge badge-model">DeepSeek</span>
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
          <div v-for="t in tickets" :key="t.ticketNo" class="ticket-card">
            <div class="ticket-head">
              <span class="ticket-no">{{ t.ticketNo }}</span>
              <el-tag size="small" :type="tagType(t.status)">{{ t.status }}</el-tag>
            </div>
            <div class="ticket-type">{{ t.type }}</div>
            <div class="ticket-title">{{ t.title }}</div>
            <div class="ticket-time">{{ formatTime(t.createdAt) }}</div>
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
            <el-icon><UploadFilled /></el-icon>
            <div>点击或拖拽文档到此处上传</div>
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
</template>

<style scoped>
.app {
  display: flex;
  height: 100%;
  min-width: 980px;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-left: 1px solid var(--border);
  border-right: 1px solid var(--border);
  background: #f7f8fb;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: var(--panel);
  border-bottom: 1px solid var(--border);
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
  color: var(--text-sub);
  background: var(--bg);
  border: 1px solid var(--border);
  padding: 3px 10px;
  border-radius: 999px;
}
.badge-model {
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  border: none;
}

.right-panel {
  width: 340px;
  background: var(--panel);
  padding: 0 4px;
  overflow-y: auto;
}
.right-tabs {
  height: 100%;
}
.right-tabs :deep(.el-tabs__content) {
  padding: 4px 14px 16px;
}
.empty-tip {
  color: var(--text-sub);
  font-size: 13px;
  text-align: center;
  padding: 40px 0;
  line-height: 1.8;
}
.ticket-card {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fafbfd;
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
  color: var(--brand);
  font-weight: 600;
}
.ticket-type {
  font-size: 12px;
  color: var(--warn);
  margin-bottom: 4px;
}
.ticket-title {
  font-size: 13px;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ticket-time {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 6px;
}
.kb-upload {
  margin-bottom: 12px;
}
.kb-hint {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 4px;
}
.doc-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--border);
}
.doc-icon {
  color: var(--brand);
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
  color: var(--text-sub);
}
</style>
