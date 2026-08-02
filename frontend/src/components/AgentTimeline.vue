<script setup>
import { Cpu } from '@element-plus/icons-vue'

defineProps({
  activity: { type: Array, default: () => [] },
  sending: { type: Boolean, default: false },
})

const INTENT_META = {
  KNOWLEDGE: { label: '知识库', color: '#2563eb' },
  BUSINESS: { label: '业务查询', color: '#c7511b' },
  TICKET: { label: '工单', color: '#b12704' },
  DIRECT: { label: '直接回复', color: '#067d62' },
  CHAIN: { label: '多步链路', color: '#7c3aed' },
}

function intentLabel(intent) {
  return (INTENT_META[intent] || { label: intent }).label
}
function intentColor(intent) {
  return (INTENT_META[intent] || { color: '#565959' }).color
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

    <div
      v-for="a in activity"
      :key="a.id"
      class="agent-card animate-in"
      :class="{ done: a.status === 'done' }"
    >
      <div class="agent-head">
        <span class="dot" :class="a.status === 'done' ? 'dot-done' : 'dot-run'"></span>
        <span class="agent-name">{{ a.name }}</span>
        <span class="agent-status" :class="a.status === 'done' ? 'st-done' : 'st-run'">
          {{ a.status === 'done' ? '完成' : '运行中' }}
        </span>
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
          <el-icon class="tool-icon" :class="t.status === 'done' ? 'tool-ok' : 'tool-run'">
            <Cpu />
          </el-icon>
          <span class="tool-name">{{ t.name }}</span>
          <span class="tool-status" :class="t.status === 'done' ? 'st-done' : 'st-run'">
            {{ t.status === 'done' ? '✓ 完成' : '调用中…' }}
          </span>
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
  border: 1px solid var(--amz-border);
  border-left: 3px solid var(--amz-orange);
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: var(--amz-card);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}
.agent-card.done {
  border-left-color: var(--amz-ok);
}
.agent-card:not(.done) {
  border-left-color: var(--amz-orange);
  animation: fadeInUp 0.4s both, breatheShadow 1.6s ease-in-out infinite;
}

@keyframes breatheShadow {
  0%, 100% { box-shadow: 0 1px 2px rgba(255, 153, 0, 0.1); }
  50%      { box-shadow: 0 1px 10px rgba(255, 153, 0, 0.28); }
}

.agent-head {
  display: flex;
  align-items: center;
}
.agent-name {
  font-size: 14px;
  font-weight: 700;
  flex: 1;
  color: var(--amz-dark);
}
.agent-status {
  font-size: 12px;
}
.st-run {
  color: var(--amz-orange-dark);
}
.st-done {
  color: var(--amz-ok);
}
.agent-title {
  font-size: 12px;
  color: var(--amz-sub);
  margin: 4px 0 6px 14px;
}

/* 路由决策（亚马逊浅黄提示） */
.route-chip {
  background: linear-gradient(180deg, #fdf8ee, #fbf1d9);
  border: 1px solid #f3d9a0;
  border-radius: 8px;
  padding: 6px 10px;
  margin: 6px 0;
  font-size: 12px;
  line-height: 1.6;
}
.route-label {
  font-weight: 700;
  margin-right: 6px;
}
.route-reason {
  color: var(--amz-sub);
}

/* 工具调用 */
.tool-item {
  border: 1px dashed var(--amz-border);
  border-radius: 8px;
  padding: 8px 10px;
  margin-top: 6px;
  background: #fdfdfc;
  transition: border-color 0.25s, box-shadow 0.25s;
}
.tool-item:has(.tool-run) {
  border-color: var(--amz-yellow);
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
  color: var(--amz-orange);
  animation: spinSlow 1.4s linear infinite;
}
.tool-ok {
  color: var(--amz-ok);
}
.tool-name {
  font-size: 13px;
  font-weight: 600;
  flex: 1;
  color: var(--amz-dark);
}
.tool-status {
  font-size: 12px;
}
.tool-args {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 11px;
  color: var(--amz-sub);
  margin-top: 6px;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f6f7f8;
  border-radius: 6px;
  padding: 5px 8px;
}
.tool-result {
  font-size: 12px;
  color: var(--amz-text);
  margin-top: 6px;
  line-height: 1.6;
  word-break: break-all;
}
.empty-tip {
  color: var(--amz-sub);
  font-size: 13px;
  text-align: center;
  padding: 40px 10px;
  line-height: 1.9;
}
</style>
