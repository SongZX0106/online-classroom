<template>
  <div class="chat-panel">
    <!-- 头部 -->
    <div class="chat-header">
      <span>在线人数：{{ onlineCount }}</span>
    </div>

    <!-- 消息列表 -->
    <el-scrollbar ref="scrollRef" class="chat-body">
      <div v-for="(msg, idx) in messages" :key="idx" class="message-card">
        <div class="msg-meta">
          <span class="name">{{ msg.user }}</span>
          <span class="time">{{ msg.time }}</span>
        </div>
        <!-- 文本+表情 -->
        <div
          v-if="msg.type === 'text'"
          class="msg-content"
          v-html="msg.content"
        ></div>
        <!-- 图片 -->
        <el-image
          v-else-if="msg.type === 'image'"
          :src="msg.content"
          :preview-src-list="[msg.content]"
          fit="cover"
          class="msg-img"
        />
      </div>
    </el-scrollbar>

    <!-- 输入区 -->
    <div class="chat-footer">
      <!-- 表情 -->
      <emoji-picker @emoji-click="insertEmoji" class="emoji-btn"></emoji-picker>
      <el-button type="primary" size="small" @click="sendText">发送</el-button>

      <!-- 文本框 -->
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="输入文字，可粘贴图片"
        @paste="handlePaste"
      />

      <!-- 图片上传 -->
      <el-upload
        ref="uploadRef"
        class="upload-btn"
        action="#"
        :show-file-list="false"
        :before-upload="beforeUpload"
        accept="image/*"
      >
        <el-button size="small" type="text">图片</el-button>
      </el-upload>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import "emoji-picker-element";

/* ------------------ 数据 ------------------ */
const onlineCount = ref(0); // 在线人数
const messages = ref([]); // 消息列表
const input = ref(""); // 输入框
const scrollRef = ref(null); // 滚动容器
const uploadRef = ref(null);

/* ------------------ 方法 ------------------ */
// 发送文本
function sendText() {
  if (!input.value.trim()) return;
  pushMessage("text", input.value.trim());
  input.value = "";
}

// 插入表情
function insertEmoji(e) {
  console.log(e);

  input.value += e.detail.unicode;
}

// 上传/粘贴图片前统一处理
function beforeUpload(file) {
  if (!file.type.startsWith("image/")) {
    ElMessage.error("只允许图片");
    return false;
  }
  toBase64(file).then((base64) => {
    pushMessage("image", base64);
  });
  return false; // 手动上传
}

// 粘贴图片
function handlePaste(e) {
  const items = (e.clipboardData || window.clipboardData).items;
  for (let i = 0; i < items.length; i++) {
    const file = items[i].getAsFile();
    if (file) {
      beforeUpload(file);
      break;
    }
  }
}

// 推到消息队列
function pushMessage(type, content) {
  messages.value.push({
    type,
    content,
    user: "我",
    time: new Date().toLocaleTimeString(),
  });
  nextTick(scrollToBottom);
}

// 滚动到底
function scrollToBottom() {
  const wrap = scrollRef.value?.wrap$ || scrollRef.value?.wrap;
  wrap && (wrap.scrollTop = wrap.scrollHeight);
}

// 文件转 base64
function toBase64(file) {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
  });
}

/* ------------------ 初始化 ------------------ */
onMounted(() => {
  // 模拟在线人数
  onlineCount.value = Math.floor(Math.random() * 100) + 200;

  // 欢迎语
  pushMessage("text", "欢迎来到直播间！");
});
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  width: 320px;
  height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
.chat-header {
  padding: 0 12px;
  background: #f5f5f5;
  line-height: 40px;
  font-size: 14px;
  color: #606266;
}
.chat-body {
  flex: 1;
  padding: 0 10px;
}
.message-card {
  margin: 10px 0;
}
.msg-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.name {
  font-weight: bold;
}
.time {
  font-size: 11px;
}
.msg-content {
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}
.msg-img {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}
.chat-footer {
  padding: 8px 10px;
  display: flex;
  align-items: flex-end;
  gap: 6px;
  background: #fafafa;
}
.emoji-btn {
  --emoji-size: 24px;
  margin-right: 4px;
}
.upload-btn {
  margin-left: auto;
}
</style>