<template>
  <!-- 右侧 AI 助手对话面板 -->
  <transition name="ai-slide">
    <section v-show="visible" class="ai-panel">
      <header class="ai-header">
        <div class="ai-title">
          <span class="ai-avatar">AI</span>
          <div class="ai-title-text">
            <div class="ai-name">导购助手</div>
            <div class="ai-status">
              <i class="dot" :class="statusClass"></i>
              {{ statusText }}
            </div>
          </div>
        </div>
        <div class="ai-header-actions">
          <el-tooltip content="清空对话" placement="bottom">
            <el-button link :icon="Delete" @click="resetChat" />
          </el-tooltip>
          <el-button link :icon="Close" @click="visible = false" />
        </div>
      </header>

      <!-- 消息列表 -->
      <div ref="bodyRef" class="ai-body">
        <div v-if="messages.length === 0" class="ai-welcome">
          <p class="welcome-title">你好，我是导购助手 👋</p>
          <p class="welcome-tip">可以帮你查商品、改商品、清空购物车、查订单。试试：</p>
          <div class="quick-list">
            <button v-for="q in quickPrompts" :key="q" class="quick-item" @click="sendText(q)">{{ q }}</button>
          </div>
        </div>

        <div v-for="(m, idx) in messages" :key="idx" class="msg" :class="m.role">
          <div v-if="m.role === 'assistant'" class="msg-avatar">AI</div>
          <div class="msg-content">
            <div v-if="m.tools && m.tools.length" class="tool-chips">
              <span v-for="(t, i) in m.tools" :key="i" class="tool-chip">
                <el-icon><Connection /></el-icon>{{ t }}
              </span>
            </div>
            <div class="bubble" :class="{ error: m.error }">
              <span v-if="m.content">{{ m.content }}</span>
              <span v-else-if="m.streaming" class="thinking">正在思考中…</span>
              <i v-if="m.streaming && m.content" class="cursor"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <footer class="ai-footer">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          resize="none"
          placeholder="问我点什么，例如：推荐 1-100 元的生活用品"
          @keydown.enter.exact.prevent="sendText()"
        />
        <el-button type="primary" :icon="Promotion" :loading="streaming" @click="sendText()">发送</el-button>
      </footer>
    </section>
  </transition>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { Delete, Close, Promotion, Connection } from '@element-plus/icons-vue'

const visible = defineModel('visible', { type: Boolean, default: false })

const messages = ref([])
const input = ref('')
const streaming = ref(false)
const connecting = ref(false)
const connected = ref(false)
const bodyRef = ref(null)
let socket = null

const quickPrompts = [
  '推荐 1-100 元的生活用品',
  '清空我的购物车',
  '看看我的已支付订单',
  '把第一个商品加入购物车'
]

const statusText = computed(() => {
  if (connected.value) return '在线 · DeepSeek'
  if (connecting.value) return '连接中…'
  return '未连接'
})
const statusClass = computed(() => ({ on: connected.value, pending: connecting.value }))

/* ---------------- 消息操作 ---------------- */

const pushAssistant = (content = '') => {
  const msg = { role: 'assistant', content, tools: [], streaming: false, error: false }
  messages.value.push(msg)
  return msg
}
const pushSystem = text => {
  messages.value.push({ role: 'assistant', content: text, tools: [], streaming: false, error: true })
}
const currentAssistant = () => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'assistant') return messages.value[i]
  }
  return null
}

// 新内容自动滚到底部
watch(messages, async () => {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}, { deep: true })

/* ---------------- WebSocket ---------------- */

const wsBase = () => (import.meta.env.VITE_API_BASE || 'http://localhost:8081').replace(/^http/, 'ws')

const connect = () => {
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return
  const token = localStorage.getItem('token')
  if (!token) {
    pushSystem('登录状态已失效，请重新登录后再使用 AI 助手。')
    return
  }
  connecting.value = true
  socket = new WebSocket(`${wsBase()}/ws/ai?token=${encodeURIComponent(token)}`)
  socket.onopen = () => {
    connected.value = true
    connecting.value = false
  }
  socket.onmessage = event => {
    try {
      handleMessage(JSON.parse(event.data))
    } catch (e) { /* 忽略非法消息 */ }
  }
  socket.onerror = () => { connecting.value = false }
  socket.onclose = () => {
    connected.value = false
    connecting.value = false
    if (streaming.value) {
      streaming.value = false
      const last = currentAssistant()
      if (last) last.streaming = false
    }
  }
}

const handleMessage = payload => {
  switch (payload.type) {
    case 'ready':
      if (!payload.configured) {
        pushSystem(payload.message)
      } else if (!messages.value.some(m => m.role === 'assistant')) {
        pushAssistant(payload.message)
      }
      break
    case 'start':
      streaming.value = true
      messages.value.push({ role: 'assistant', content: '', tools: [], streaming: true, error: false })
      break
    case 'reset':
      pushAssistant('对话已清空，我们重新开始吧～有什么可以帮你？')
      break
    case 'tool': {
      const last = currentAssistant()
      if (last) last.tools = [...(last.tools || []), payload.name]
      break
    }
    case 'delta': {
      const last = currentAssistant()
      if (last) last.content += payload.content
      break
    }
    case 'done': {
      streaming.value = false
      const last = currentAssistant()
      if (last) last.streaming = false
      // 工具改动过数据：通知其他页面刷新（购物车角标、商品/订单/统计列表）
      if (payload.dataChanged) window.dispatchEvent(new CustomEvent('sg:data-changed'))
      break
    }
    case 'error': {
      streaming.value = false
      const last = currentAssistant()
      if (last && last.streaming) {
        last.streaming = false
        if (!last.content) last.content = payload.message
        else last.content += '\n' + payload.message
      } else {
        pushSystem(payload.message)
      }
      break
    }
    default:
      break
  }
}

// 等待连接就绪（首次打开面板即发送时可能还在握手）
const waitOpen = timeout => new Promise(resolve => {
  const start = Date.now()
  const timer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      clearInterval(timer)
      resolve(true)
    } else if (Date.now() - start > timeout) {
      clearInterval(timer)
      resolve(false)
    }
  }, 80)
})

const sendText = async text => {
  const content = (text !== undefined ? text : input.value).trim()
  if (!content || streaming.value) return
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    connect()
    const ok = await waitOpen(5000)
    if (!ok) {
      pushSystem('连接 AI 服务失败，请确认后端已启动。')
      return
    }
  }
  messages.value.push({ role: 'user', content })
  input.value = ''
  socket.send(JSON.stringify({ message: content }))
}

const resetChat = () => {
  messages.value = []
  if (socket && socket.readyState === WebSocket.OPEN) {
    // 同时清空服务端会话记忆，避免模型还记得上一轮对话
    socket.send(JSON.stringify({ type: 'reset' }))
  } else {
    connect()
  }
}

// 打开面板即建立连接，之后保持长连接
watch(visible, val => {
  if (val) connect()
})

onBeforeUnmount(() => {
  if (socket) socket.close()
  socket = null
})
</script>

<style scoped>
.ai-panel {
  position: fixed;
  top: var(--sg-header-height);
  right: 0;
  bottom: 0;
  width: 390px;
  background: #fff;
  box-shadow: -8px 0 28px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 2000;
  border-left: 1px solid #f0f0f0;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #f2f2f2;
  background: linear-gradient(90deg, #fff6f1, #ffffff);
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6a3c, #f5541f);
  color: #fff;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(245, 84, 31, 0.3);
}

.ai-name {
  font-size: 15px;
  font-weight: bold;
  color: #333;
}

.ai-status {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #ccc;
  display: inline-block;
}

.dot.on {
  background: #52c41a;
}

.dot.pending {
  background: #faad14;
}

.ai-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  background: #fafafa;
}

.ai-welcome {
  padding: 6px 2px 18px;
}

.welcome-title {
  font-size: 15px;
  font-weight: bold;
  color: #333;
  margin: 0 0 6px;
}

.welcome-tip {
  font-size: 13px;
  color: #999;
  margin: 0 0 12px;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-item {
  text-align: left;
  padding: 9px 12px;
  border-radius: 8px;
  border: 1px solid #ffe0d2;
  background: #fff;
  color: #f5541f;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-item:hover {
  background: var(--sg-primary-light);
  border-color: #ffb696;
}

.msg {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.msg.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6a3c, #f5541f);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.msg-content {
  max-width: 78%;
}

.msg.user .msg-content {
  display: flex;
  justify-content: flex-end;
}

.tool-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.tool-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #f5541f;
  background: #fff4ee;
  border: 1px solid #ffd9c6;
  border-radius: 10px;
  padding: 2px 8px;
}

.bubble {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.user .bubble {
  background: linear-gradient(135deg, #ff6a3c, #f5541f);
  color: #fff;
  border: none;
}

.bubble.error {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fde2e2;
}

.thinking {
  color: #999;
}

.cursor {
  display: inline-block;
  width: 7px;
  height: 15px;
  background: #f5541f;
  margin-left: 2px;
  vertical-align: -2px;
  animation: blink 0.9s steps(2, start) infinite;
}

@keyframes blink {
  to { visibility: hidden; }
}

.ai-footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid #f2f2f2;
  background: #fff;
}

.ai-footer :deep(.el-textarea__inner) {
  border-radius: 8px;
  font-size: 14px;
}

.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: transform 0.25s ease;
}

.ai-slide-enter-from,
.ai-slide-leave-to {
  transform: translateX(100%);
}
</style>
