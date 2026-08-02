<script setup>
import { ref, watch, nextTick } from 'vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  sending: { type: Boolean, default: false },
  suggestions: { type: Array, default: () => [] },
})
const emit = defineEmits(['send'])
const input = defineModel()

const scrollRef = ref(null)

async function scrollBottom() {
  await nextTick()
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}

watch(
  () => (props.messages.length ? props.messages[props.messages.length - 1].content : ''),
  scrollBottom,
)

function doSend() {
  emit('send')
}

function useSuggest(text) {
  input.value = text
  emit('send')
}
</script>

<template>
  <div class="chat-panel">
    <div ref="scrollRef" class="msg-area">
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-logo">Co</div>
        <div class="welcome-title">CoAgent 智能客服 · 多智能体协作</div>
        <div class="welcome-sub">
          编排者(Supervisor) 调度 知识库 / 业务查询 / 工单 三个专精 Agent，实时可视化运行过程。
        </div>
        <div class="suggests">
          <button v-for="s in suggestions" :key="s" class="suggest" @click="useSuggest(s)">
            {{ s }}
          </button>
        </div>
      </div>

      <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role === 'USER' ? 'row-user' : 'row-ai'">
        <div v-if="m.role === 'ASSISTANT'" class="avatar avatar-ai">优</div>
        <div class="bubble" :class="m.role === 'USER' ? 'bubble-user' : 'bubble-ai'">
          <span
            v-if="sending && i === messages.length - 1 && m.role === 'ASSISTANT'"
            class="stream-cursor"
          >{{ m.content }}</span>
          <template v-else>{{ m.content || (i === messages.length - 1 ? '思考中…' : '') }}</template>
        </div>
        <div v-if="m.role === 'USER'" class="avatar avatar-user">我</div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="输入您的问题，Enter 发送，Shift+Enter 换行"
        @keydown.enter.exact.prevent="doSend"
      />
      <div class="input-bar">
        <span class="tip">{{ sending ? '🔄 Agent 正在处理…' : '多智能体协作 · SSE 流式回复' }}</span>
        <el-button type="primary" :loading="sending" @click="doSend">发送</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.msg-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.welcome {
  text-align: center;
  padding: 60px 20px;
}
.welcome-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
  font-size: 26px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.3);
}
.welcome-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
}
.welcome-sub {
  color: var(--text-sub);
  font-size: 13px;
  margin-bottom: 26px;
  line-height: 1.8;
}
.suggests {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 560px;
  margin: 0 auto;
}
.suggest {
  border: 1px solid var(--border);
  background: var(--panel);
  color: var(--text);
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.suggest:hover {
  border-color: var(--brand);
  color: var(--brand);
  box-shadow: var(--shadow);
}
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  align-items: flex-start;
}
.row-user {
  justify-content: flex-end;
}
.row-ai {
  justify-content: flex-start;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}
.avatar-ai {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
}
.avatar-user {
  background: #e2e8f0;
  color: #475569;
}
.bubble {
  max-width: 68%;
}
.input-area {
  padding: 14px 24px 18px;
  border-top: 1px solid var(--border);
  background: var(--panel);
}
.input-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.tip {
  font-size: 12px;
  color: var(--text-sub);
}
</style>
