<template>
  <div class="blue-chat-panel" @paste="handlePaste">
    <!-- 头部 -->
    <header class="chat-header">
      <span class="title">会中聊天</span>
    </header>

    <!-- 聊天内容 -->
    <main ref="scrollRef" class="chat-body">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['msg-row', msg.user === '我' ? 'self' : 'other']"
      >
        <div class="msg-content">
          <span class="user" :class="{ me: msg.user === '我' }">
            {{ msg.user }}：
          </span>
          <span v-if="msg.type === 'text'" v-html="msg.content"></span>
          <img v-else-if="msg.type === 'image'" :src="msg.content" class="chat-image" />
        </div>
      </div>
    </main>

    <!-- 输入区域 -->
    <footer class="chat-footer">
      <div class="footer-middle">
        <el-input
          v-model="input"
          placeholder="请输入消息..."
          @keydown.enter.prevent="sendText"
          class="input-blue"
          type="textarea"
          :rows="5"
        />
      </div>

      <div class="footer-bottom">
        <el-icon @click="toggleEmoji"><Smile /></el-icon>
        <el-icon><Picture /></el-icon>
        <div @click="sendText" class="send-button">发送</div>
      </div>

      <!-- 表情弹窗 -->
      <emoji-picker
        v-if="showEmoji"
        class="emoji-picker"
        @emoji-click="insertEmoji"
      />
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import 'emoji-picker-element'
import { Smile, Picture } from '@element-plus/icons-vue'

const messages = ref([])
const input = ref('')
const scrollRef = ref(null)
const showEmoji = ref(false)

function sendText() {
  if (!input.value.trim()) return
  pushMessage('text', input.value.trim())
  input.value = ''
  showEmoji.value = false
}

function insertEmoji(e) {
  input.value += e.detail.unicode
  nextTick(() => {
    showEmoji.value = false
  })
}

function toggleEmoji() {
  showEmoji.value = !showEmoji.value
}

function beforeUpload(file) {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只允许图片')
    return false
  }
  toBase64(file).then((base64) => {
    pushMessage('image', base64)
  })
  return false
}

function handlePaste(e) {
  const items = (e.clipboardData || window.clipboardData).items
  for (let i = 0; i < items.length; i++) {
    const file = items[i].getAsFile()
    if (file) {
      beforeUpload(file)
      break
    }
  }
}

function pushMessage(type, content) {
  messages.value.push({
    type,
    content,
    user: '我',
    time: new Date().toLocaleTimeString()
  })
  nextTick(scrollToBottom)
}

function scrollToBottom() {
  const wrap = scrollRef.value?.$el || scrollRef.value
  wrap && (wrap.scrollTop = wrap.scrollHeight)
}

function toBase64(file) {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => resolve(reader.result)
  })
}

onMounted(() => {
  pushMessage('text', '欢迎来到直播间！')
})
</script>

<style scoped>
.blue-chat-panel {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #e0f2ff;
  color: #1e3a8a;
  font-family: 'Segoe UI', sans-serif;
  position: relative;
}

.chat-header {
  height: 40px;
  padding: 0 12px;
  background: #1e3a8a;
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #fff;
  border-bottom: 1px solid #1e40af;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.msg-row {
  margin-bottom: 8px;
}
.msg-content {
  font-size: 14px;
  color: #1e3a8a;
  line-height: 1.5;
}
.msg-content .user {
  color: #3b82f6;
  font-weight: 500;
}
.msg-content .user.me {
  color: #2563eb;
}
.chat-image {
  max-width: 160px;
  border-radius: 6px;
  margin-top: 4px;
}

/* footer */
.chat-footer {
  background: #dbeafe;
  border-top: 1px solid #93c5fd;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.footer-middle {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}
.input-blue textarea {
  background: #e0f2ff !important;
  color: #1e3a8a;
  border: 1px solid #60a5fa !important;
  border-radius: 4px;
}
.footer-bottom {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
  color: #3b82f6;
  font-size: 18px;
}
.send-button {
  background-color: #3b82f6;
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
}
.send-button:hover {
  background-color: #2563eb;
}

.emoji-picker {
  position: absolute;
  right: 12px;
  bottom: 120px;
  z-index: 1000;
  max-height: 300px;
  max-width: 320px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
  border-radius: 6px;
  background: white;
}
</style>
