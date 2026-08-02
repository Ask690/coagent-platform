<script setup>
import { Cpu } from '@element-plus/icons-vue'

defineProps({
  activity: { type: Array, default: () => [] },
  sending: { type: Boolean, default: false },
})

const INTENT_META = {
  KNOWLEDGE: { label: '知识库', color: '#2563eb' },
  BUSINESS: { label: '业务查询', color: '#d97706' },
  TICKET: { label: '工单', color: '#dc2626' },
  DIRECT: { label: '直接回复', color: '#16a34a' },
  CHAIN: { label: '多步链路', color: '#7c3aed' },
}

function intentLabel(intent) {
  return (INTENT_META[intent] || { label: intent }).label
}
function intentColor(intent) {
  return (INTENT_META[intent] || { color: '#6b7280' }).color
}
function fmtArgs(args) {
  if (!args) return ''
  try {
    return JSON.stringify(args, null, 1).replace(/"([^"]+)":/g, '$1: ')
  } catch {
    return String(args)
  }
}
function short(text) {
  const s = String(text || '')
  return s.length > 110 ? s.slice(0, 110) + '…' : s
}
</script>

<template>
  <div class="timeline">
    <div v-if="!activity.length" class="empty-tip">
      {{ sending ? '🧠 编排者正在分析意图…' : '等待对话，这里将实时展示每个 Agent 的工作过程' }}
    </div>

    <div v-for="a in activity" :key="a.id" class="agent-card" :class="{ done: a.status === 'done' }">
      <div class="agent-head">
        <span class="dot" :class="a.status === 'done' ? 'dot-done' : 'dot-run'"></span>
        <span class="agent-name">{{ a.name }}</span>
        <span class="agent-status">{{ a.status === 'done' ? '完成' : '运行中' }}</span>
      </div>
      <div class="agent-title">{{ a.title }}</div>

      <div v-if="a.route" class="route-chip">
        <span class="route-label" :style="{ color: intentColor(a.route.intent) }">
          → 分派给 {{ intentLabel(a.route.intent) }}
        </span>
        <span class="route-reason">{{ a.route.reason }}</span>
      </div>

      <div v-for="(t, i) in a.tools" :key="i" class="tool-item">
        <div class="tool-head">
          <el-icon class="tool-icon" :class="t.status === 'done' ? 'tool-ok' : 'tool-run'"><Cpu /></el-icon>
          <span class="tool-name">{{ t.name }}</span>
          <span class="tool-status">{{ t.status === 'done' ? '✓' : '…' }}</span>
        </div>
        <div v-if="t.args" class="tool-args">{{ fmtArgs(t.args) }}</div>
        <div v-if="t.result" class="tool-result">{{ short(t.result) }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline {
  padding: 4px 2px;
}
.agent-card {
  border: 1px solid var(--border);
  border-left: 3px solid var(--run);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: #fafbfd;
  transition: opacity 0.3s;
}
.agent-card.done {
  border-left-color: var(--ok);
}
.agent-head {
  display: flex;
  align-items: center;
}
.agent-name {
  font-size: 14px;
  font-weight: 600;
  flex: 1;
}
.agent-status {
  font-size: 12px;
  color: var(--text-sub);
}
.agent-title {
  font-size: 12px;
  color: var(--text-sub);
  margin: 4px 0 6px 14px;
}
.route-chip {
  background: #eef2ff;
  border-radius: 8px;
  padding: 6px 10px;
  margin: 6px 0;
  font-size: 12px;
  line-height: 1.6;
}
.route-label {
  font-weight: 600;
  margin-right: 6px;
}
.route-reason {
  color: var(--text-sub);
}
.tool-item {
  border: 1px dashed var(--border);
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 6px;
  background: #fff;
}
.tool-head {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tool-icon {
  font-size: 15px;
}
.tool-run {
  color: var(--run);
  animation: pulse 1.2s infinite;
}
.tool-ok {
  color: var(--ok);
}
.tool-name {
  font-size: 13px;
  font-weight: 500;
  flex: 1;
}
.tool-status {
  font-size: 12px;
}
.tool-args {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  color: var(--text-sub);
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f6f7f9;
  border-radius: 6px;
  padding: 5px 8px;
}
.tool-result {
  font-size: 12px;
  color: var(--text);
  margin-top: 6px;
  line-height: 1.6;
  word-break: break-all;
}
.empty-tip {
  color: var(--text-sub);
  font-size: 13px;
  text-align: center;
  padding: 40px 10px;
  line-height: 1.9;
}
</style>
