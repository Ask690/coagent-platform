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
      <!-- 欢迎页 -->
      <div v-if="!messages.length" class="welcome">
        <div class="welcome-orb">Co</div>
        <div class="welcome-title">CoAgent 智能客服 · 多智能体协作</div>
        <div class="welcome-sub">
          编排者(Supervisor) 调度 知识库 / 业务查询 / 工单 三个专精 Agent，实时可视化运行过程。
        </div>
        <div class="suggests">
          <button
            v-for="(s, i) in suggestions"
            :key="s"
            class="suggest"
            :style="{ animationDelay: (i * 120) + 'ms' }"
            @click="useSuggest(s)"
          >
            <span class="suggest-icon">✦</span>{{ s }}
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div
        v-for="(m, i) in messages"
        :key="i"
        class="msg-row animate-in"
        :class="m.role === 'USER' ? 'row-user' : 'row-ai'"
      >
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

    <!-- 输入区 -->
    <div class="input-area">
      <div class="input-box">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入您的问题，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="doSend"
        />
      </div>
      <div class="input-bar">
        <span class="tip" :class="{ sending }">
          {{ sending ? '🔄 Agent 正在处理…' : '多智能体协作 · SSE 流式回复' }}
        </span>
        <button class="send-btn" :class="{ loading: sending }" :disabled="sending" @click="doSend">
          <span v-if="sending" class="send-spinner"></span>
          <span>{{ sending ? '处理中' : '发送' }}</span>
        </button>
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

/* ===== 欢迎页 ===== */
.welcome {
  text-align: center;
  padding: 54px 20px 30px;
  animation: fadeInUp 0.6s both;
}
.welcome-orb {
  width: 70px;
  height: 70px;
  margin: 0 auto 18px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  font-weight: 800;
  color: var(--amz-dark);
  background: linear-gradient(135deg, var(--amz-yellow), var(--amz-orange), #ff8c1a);
  background-size: 200% 200%;
  animation: gradShift 4s ease infinite, floatY 5s ease-in-out infinite;
  box-shadow: 0 10px 30px rgba(255, 153, 0, 0.4);
}
.welcome-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--amz-dark);
}
.welcome-sub {
  color: var(--amz-sub);
  font-size: 13px;
  margin-bottom: 26px;
  line-height: 1.8;
}
.suggests {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 620px;
  margin: 0 auto;
}
.suggest {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--amz-border);
  background: var(--amz-card);
  color: var(--amz-text);
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  animation: fadeInUp 0.5s both;
  transition: all 0.22s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.suggest:hover {
  transform: translateY(-3px);
  border-color: var(--amz-orange);
  color: var(--amz-orange-dark);
  box-shadow: 0 6px 18px rgba(255, 153, 0, 0.22);
}
.suggest-icon {
  color: var(--amz-orange);
  font-size: 12px;
}

/* ===== 消息行 ===== */
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.row-user { justify-content: flex-end; }
.row-ai   { justify-content: flex-start; }

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
  color: #fff;
}
.avatar-ai {
  background: linear-gradient(135deg, var(--amz-dark-2), #1d4ed8);
  box-shadow: 0 2px 6px rgba(19, 25, 33, 0.3);
}
.avatar-user {
  background: var(--amz-dark-3);
  color: var(--amz-yellow);
}

.bubble {
  max-width: 68%;
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-user {
  background: linear-gradient(135deg, var(--amz-dark), var(--amz-dark-2));
  color: #fff;
  border-radius: 14px 4px 14px 14px;
  padding: 11px 15px;
  box-shadow: 0 3px 10px rgba(19, 25, 33, 0.25);
}
.bubble-ai {
  background: var(--amz-card);
  color: var(--amz-text);
  border: 1px solid var(--amz-border);
  border-left: 3px solid var(--amz-orange);
  border-radius: 4px 14px 14px 14px;
  padding: 11px 15px;
  box-shadow: var(--shadow);
  transition: box-shadow 0.25s, transform 0.25s;
}
.bubble-ai:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-1px);
}

/* ===== 输入区 ===== */
.input-area {
  padding: 12px 24px 16px;
  border-top: 1px solid var(--amz-border);
  background: var(--amz-card);
}
.input-box :deep(.el-textarea__inner) {
  border: 1.5px solid var(--amz-border);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.6;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-box :deep(.el-textarea__inner:focus) {
  border-color: var(--amz-orange);
  box-shadow: 0 0 0 3px rgba(255, 153, 0, 0.15);
}
.input-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.tip {
  font-size: 12px;
  color: var(--amz-sub);
  transition: color 0.2s;
}
.tip.sending {
  color: var(--amz-warn);
}

/* 发送按钮（亚马逊橙 + 渐变流动） */
.send-btn {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 26px;
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
  box-shadow: 0 2px 8px rgba(255, 153, 0, 0.35);
}
.send-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 0 0 5px rgba(255, 153, 0, 0.18);
}
.send-btn:active:not(:disabled) {
  transform: scale(0.97);
}
.send-btn:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}
.send-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(19, 25, 33, 0.3);
  border-top-color: var(--amz-dark);
  border-radius: 50%;
  animation: spinSlow 0.7s linear infinite;
}
</style>
