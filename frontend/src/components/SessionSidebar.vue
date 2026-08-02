<script setup>
import { Plus } from '@element-plus/icons-vue'

defineProps({
  sessions: { type: Array, default: () => [] },
  currentId: { type: String, default: null },
})
defineEmits(['create', 'select'])

function formatTime(iso) {
  if (!iso) return ''
  return iso.replace('T', ' ').slice(5, 16)
}
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <div class="logo">Co</div>
      <div>
        <div class="brand-name">CoAgent</div>
        <div class="brand-sub">多智能体客服平台</div>
      </div>
    </div>

    <el-button type="primary" class="new-btn" @click="$emit('create')">
      <el-icon><Plus /></el-icon><span>新建会话</span>
    </el-button>

    <div class="list">
      <div
        v-for="s in sessions"
        :key="s.sessionId"
        class="item"
        :class="{ active: s.sessionId === currentId }"
        @click="$emit('select', s.sessionId)"
      >
        <div class="item-title">{{ s.title }}</div>
        <div class="item-time">{{ formatTime(s.updatedAt) }}</div>
      </div>
      <div v-if="!sessions.length" class="empty">暂无会话，点击上方新建</div>
    </div>

    <div class="footer-tip">RAG 检索 · 工具调用 · 工单流转</div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 250px;
  background: #0f172a;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 18px 14px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 18px;
}
.logo {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
}
.brand-name {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}
.brand-sub {
  font-size: 12px;
  color: #94a3b8;
}
.new-btn {
  width: 100%;
  margin-bottom: 14px;
}
.list {
  flex: 1;
  overflow-y: auto;
}
.item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.item:hover {
  background: rgba(148, 163, 184, 0.12);
}
.item.active {
  background: rgba(59, 130, 246, 0.25);
  box-shadow: inset 3px 0 0 #3b82f6;
}
.item-title {
  font-size: 13px;
  color: #f1f5f9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-time {
  font-size: 11px;
  color: #64748b;
  margin-top: 3px;
}
.empty {
  color: #64748b;
  font-size: 12px;
  text-align: center;
  padding: 30px 0;
}
.footer-tip {
  font-size: 11px;
  color: #475569;
  text-align: center;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}
</style>
