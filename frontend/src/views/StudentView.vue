<template>
  <div class="student-container">
    <!-- 姓名输入页面 -->
    <div v-if="!hasJoined" class="name-input-container">
      <div class="login-card">
        <h2>加入在线课堂</h2>
        <div class="input-group">
          <el-input 
            v-model="studentName" 
            placeholder="请输入你的姓名" 
            maxlength="20"
            :prefix-icon="User"
            @keyup.enter="joinClass"
          />
          <el-button type="primary" @click="joinClass" :disabled="!studentName.trim()">
            进入课堂
          </el-button>
        </div>
        <p class="tip" v-if="nameError">{{ nameError }}</p>
      </div>
    </div>
    
    <!-- 课程内容页面 -->
    <div v-else-if="hasJoined && !showErrorPage" class="class-container">
      <div class="header">
        <div class="logo">
          <h2>WebRTC 在线课堂</h2>
        </div>
        <div class="status-bar">
          <el-tag type="success" effect="dark" v-if="connected">
            <el-icon><VideoCamera /></el-icon>
            已连接
          </el-tag>
          <el-tag type="warning" effect="dark" v-else>
            <el-icon><Loading /></el-icon>
            {{ statusMessage }}
          </el-tag>
          <el-tag type="info" effect="dark" class="student-count">
            <el-icon><User /></el-icon>
            当前在线: {{ studentCount }} 人
          </el-tag>
        </div>
      </div>
      
      <div class="video-wrapper">
        <div class="video-container">
          <video ref="videoRef" autoplay controls />
          <div v-if="!connected" class="connection-status">
            <el-icon class="loading-icon"><Loading /></el-icon>
            {{ statusMessage }}
          </div>
        </div>
      </div>
    </div>
    
    <!-- 错误页面 -->
    <div v-else class="error-container">
      <div class="error-card">
        <el-result
          :icon="errorInfo.icon"
          :title="errorInfo.title"
          :sub-title="errorInfo.message"
        >
          <template #extra>
            <el-button type="primary" @click="retryConnection">重新连接</el-button>
            <el-button @click="goHome">返回首页</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, onBeforeUnmount, computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { io } from 'socket.io-client'
import SimplePeer from 'simple-peer'
import { ElMessage } from 'element-plus'
import { User, VideoCamera, Loading, Warning, CircleCloseFilled } from '@element-plus/icons-vue'

const socket = ref(null)
const videoRef = ref(null)
const route = useRoute()
const router = useRouter()

// 状态管理
const connected = ref(false)
const statusMessage = ref('正在连接老师...')
const onlineUsers = ref([])
const hasJoined = ref(false)
const showErrorPage = ref(false)
const studentName = ref('')
const nameError = ref('')

// 错误信息
const errorInfo = reactive({
  icon: 'warning',
  title: '连接错误',
  message: '连接已断开，请重新尝试'
})

const studentCount = computed(() => {
  return onlineUsers.value.filter(u => u.role === 'student').length
})

// 加入课堂
function joinClass() {
  if (!studentName.value.trim()) {
    nameError.value = '请输入姓名'
    return
  }
  
  nameError.value = ''
  hasJoined.value = true
  initializeConnection()
}

// 初始化连接
function initializeConnection() {
  const roomId = route.params.roomId
  
  // 创建新的socket连接
  socket.value = io('http://localhost:3000')
  
  // 监听房间用户变化
  socket.value.on('room-users', (users) => {
    console.log('房间用户更新:', users)
    onlineUsers.value = users
  })
  
  // 监听被踢出
  socket.value.on('kicked', () => {
    ElMessage.error('您已被老师移出课堂')
    showErrorPage.value = true
    errorInfo.icon = 'error'
    errorInfo.title = '已被移出课堂'
    errorInfo.message = '您已被老师移出课堂，如有疑问请联系老师'
    
    // 断开连接
    disconnectAll()
  })
  
  // 监听课程结束
  socket.value.on('class-ended', () => {
    ElMessage.info('课程已结束')
    showErrorPage.value = true
    errorInfo.icon = 'info'
    errorInfo.title = '课程已结束'
    errorInfo.message = '老师已结束课程，感谢您的参与'
    
    // 断开连接
    disconnectAll()
  })
  
  socket.value.emit('join-room', { roomId, userName: studentName.value })

  // 创建一个变量存储 peer 实例
  let peer = null

  // 先监听信号，等待老师的第一个信号后再创建 peer
  socket.value.on('signal', ({ from, signal }) => {
    console.log('收到老师信号:', from, signal)
    
    try {
      if (!peer) {
        console.log('创建学生端 Peer')
        // 第一次收到信号时才创建 peer，添加更多选项增强兼容性
        peer = new SimplePeer({
          initiator: false, // 设置为接收者
          trickle: false, // 禁用 trickle ICE
          objectMode: true, // 启用对象模式
          sdpTransform: (sdp) => sdp // 禁用 SDP 转换
        })
        
        peer.on('signal', data => {
          console.log('发送信号给老师:', from)
          socket.value.emit('signal', { to: from, from: socket.value.id, signal: data })
        })
        
        peer.on('stream', stream => {
          console.log('收到视频流')
          videoRef.value.srcObject = stream
          connected.value = true
          statusMessage.value = '已连接'
        })
        
        peer.on('error', err => {
          console.error('Peer 错误:', err)
          statusMessage.value = '连接错误，请刷新页面重试'
        })
        
        peer.on('close', () => {
          console.log('连接已关闭')
          connected.value = false
          statusMessage.value = '连接已断开'
        })
      }
      
      // 处理收到的信号
      peer.signal(signal)
    } catch (err) {
      console.error('处理信号错误:', err)
      statusMessage.value = '连接错误，请刷新页面重试'
    }
  })
}

// 重试连接
function retryConnection() {
  showErrorPage.value = false
  initializeConnection()
}

// 返回首页
function goHome() {
  router.push('/')
}

// 断开所有连接
function disconnectAll() {
  if (socket.value) {
    socket.value.disconnect()
  }
  
  if (videoRef.value && videoRef.value.srcObject) {
    const tracks = videoRef.value.srcObject.getTracks()
    tracks.forEach(track => track.stop())
    videoRef.value.srcObject = null
  }
  
  connected.value = false
}

// 组件卸载前清理资源
onBeforeUnmount(() => {
  disconnectAll()
})
</script>

<style scoped>
.student-container {
  width: 100%;
  height: 100vh;
  background-color: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 姓名输入页面 */
.name-input-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #1c92d2, #f2fcfe);
}

.login-card {
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.login-card h2 {
  color: #409EFF;
  margin-bottom: 20px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.tip {
  color: #f56c6c;
  font-size: 12px;
  margin-top: 10px;
}

/* 课程内容页面 */
.class-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: #fff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.logo h2 {
  margin: 0;
  color: #409EFF;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-count {
  margin-left: 10px;
}

.video-wrapper {
  flex: 1;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.video-container {
  position: relative;
  width: 100%;
  max-width: 1200px;
  height: calc(100vh - 120px);
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.connection-status {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 18px;
  background-color: rgba(0, 0, 0, 0.7);
  padding: 20px 30px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.loading-icon {
  font-size: 24px;
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 错误页面 */
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f7fa;
}

.error-card {
  background-color: white;
  border-radius: 8px;
  padding: 30px;
  width: 500px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-card, .error-card {
    width: 90%;
    padding: 20px;
  }
  
  .video-container {
    height: calc(100vh - 150px);
  }
}
</style>
