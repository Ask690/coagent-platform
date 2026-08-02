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
      <div class="logo-orb">
        <span>Co</span>
      </div>
      <div class="brand-name">
        Co<span class="accent">Agent</span>
        <div class="brand-sub">智能客服 · 多智能体</div>
      </div>
    </div>

    <button class="new-btn" @click="$emit('create')">
      <el-icon><Plus /></el-icon>
      <span>新建会话</span>
      <span class="btn-glow"></span>
    </button>

    <div class="list">
      <div
        v-for="(s, i) in sessions"
        :key="s.sessionId"
        class="item"
        :class="{ active: s.sessionId === currentId }"
        :style="{ animationDelay: (i * 40) + 'ms' }"
        @click="$emit('select', s.sessionId)"
      >
        <div class="item-bar"></div>
        <div class="item-body">
          <div class="item-title">{{ s.title }}</div>
          <div class="item-time">{{ formatTime(s.updatedAt) }}</div>
        </div>
      </div>
      <div v-if="!sessions.length" class="empty">暂无会话，点击上方新建</div>
    </div>

    <div class="footer-tip">
      <span class="foot-dot"></span>RAG 检索 · 工具调用 · 工单流转
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 258px;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--amz-dark), #0b0f14);
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  padding: 16px 14px;
}

/* 品牌 */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px 16px;
}
.logo-orb {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 800;
  color: var(--amz-dark);
  background: linear-gradient(135deg, var(--amz-yellow), var(--amz-orange));
  background-size: 200% 200%;
  animation: gradShift 5s ease infinite;
  box-shadow: 0 4px 14px rgba(255, 153, 0, 0.35);
}
.brand-name {
  font-size: 17px;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}
.brand-name .accent {
  color: var(--amz-orange);
}
.brand-sub {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
  margin-top: 2px;
}

/* 新建按钮（亚马逊 Add-to-Cart 风格） */
.new-btn {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--amz-dark);
  background: linear-gradient(135deg, var(--amz-orange), #ffb643, var(--amz-orange));
  background-size: 200% 200%;
  animation: gradShift 4s ease infinite;
  transition: all 0.22s;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(255, 153, 0, 0.3);
}
.new-btn:hover {
  transform: translateY(-1px) scale(1.01);
  box-shadow: 0 0 0 5px rgba(255, 153, 0, 0.18);
}
.new-btn:active {
  transform: scale(0.98);
}
.btn-glow {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 40px;
  background: linear-gradient(105deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  left: -60px;
  animation: shimmer 2.6s ease-in-out infinite;
}

/* 会话列表 */
.list {
  flex: 1;
  overflow-y: auto;
}
.item {
  position: relative;
  display: flex;
  align-items: stretch;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  overflow: hidden;
  animation: fadeInUp 0.4s both;
  transition: background 0.2s, transform 0.2s;
}
.item:hover {
  background: var(--amz-dark-3);
  transform: translateX(2px);
}
.item.active {
  background: #0d1b2a;
}
.item-bar {
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 2px;
  background: transparent;
  transition: background 0.25s;
}
.item.active .item-bar {
  background: linear-gradient(180deg, var(--amz-orange), var(--amz-yellow));
}
.item-body {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-size: 13px;
  color: #f1f5f9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item.active .item-title {
  color: #fff;
  font-weight: 600;
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
  color: #64748b;
  text-align: center;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.foot-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #48d98a;
  animation: breathe 2s infinite;
}
</style>
