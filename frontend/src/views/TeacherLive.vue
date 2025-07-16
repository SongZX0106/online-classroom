<template>
    <div class="teacher-container">
        <div class="header">
            <div class="logo">
                <h2>WebRTC 在线课堂</h2>
            </div>
            <div class="controls">
                <el-button type="primary" @click="startClass" v-if="!isClassStarted" :icon="VideoPlay">开始上课</el-button>
                <el-button type="danger" @click="endClass" v-if="isClassStarted" :icon="VideoPause">停止上课</el-button>
                <div class="status-info" v-if="isClassStarted">
                    <el-icon>
                        <User />
                    </el-icon>
                    当前在线: {{ studentCount }} 人
                </div>
            </div>
        </div>

        <div class="main-content" :class="{ 'class-active': isClassStarted }">
            <div class="screen-container">
                <div v-if="!isClassStarted" class="placeholder">
                    <el-empty description="点击开始上课按钮开始课程">
                        <el-button type="primary" @click="startClass">开始上课</el-button>
                    </el-empty>
                </div>

                <div v-if="isClassStarted" class="video-content">
                    <video ref="videoRef" autoplay muted />
                </div>

                <div class="share-link" v-if="classUrl && isClassStarted">
                    <el-alert title="学生上课链接" type="info" :closable="false" show-icon>
                        <div class="link-content">
                            <span class="link-text">{{ classUrl }}</span>
                            <el-button size="small" type="primary" @click="copyLink" :icon="CopyDocument">复制</el-button>
                            <el-button size="small" @click="openLink" :icon="Link">打开</el-button>
                        </div>
                    </el-alert>
                </div>
            </div>

            <div class="students-panel" v-if="isClassStarted">
                <div class="panel-header">
                    <h3>
                        <el-icon>
                            <User />
                        </el-icon>
                        在线学生
                    </h3>
                </div>

                <el-scrollbar height="calc(100% - 50px)">
                    <div class="student-list">
                        <el-empty description="暂无学生" v-if="!onlineUsers.filter(u => u.role === 'student').length" />
                        <div v-for="user in onlineUsers.filter(u => u.role === 'student')" :key="user.id"
                            class="student-item">
                            <div class="student-info">
                                <el-avatar :size="32" :icon="UserFilled" />
                                <span class="student-name">{{ user.name }}</span>
                            </div>
                            <el-button type="danger" size="small" @click="kickStudent(user.id)"
                                :icon="Delete">踢出</el-button>
                        </div>
                    </div>
                </el-scrollbar>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { io } from 'socket.io-client'
import SimplePeer from 'simple-peer'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    VideoPlay,
    VideoPause,
    User,
    UserFilled,
    Delete,
    CopyDocument,
    Link
} from '@element-plus/icons-vue'

const socket = io('http://localhost:3000')
const videoRef = ref(null)
const classUrl = ref('')
const currentRoomId = ref('')
const isClassStarted = ref(false)
const onlineUsers = ref([])
let peers = {}
let stream = null

const studentCount = computed(() => {
    return onlineUsers.value.filter(u => u.role === 'student').length
})

// 复制链接
function copyLink() {
    if (classUrl.value) {
        navigator.clipboard.writeText(classUrl.value)
            .then(() => ElMessage.success('链接已复制到剪贴板'))
            .catch(() => ElMessage.error('复制失败，请手动复制'))
    }
}

// 打开链接
function openLink() {
    if (classUrl.value) {
        window.open(classUrl.value, '_blank')
    }
}

// 监听房间用户变化
function setupRoomListeners() {
    socket.on('room-users', (users) => {
        console.log('房间用户更新:', users)
        onlineUsers.value = users
    })
}

// 获取房间用户列表
function getRoomUsers() {
    if (currentRoomId.value) {
        socket.emit('get-room-users', currentRoomId.value, (users) => {
            onlineUsers.value = users
        })
    }
}

// 开始上课
async function startClass() {
    try {
        stream = await navigator.mediaDevices.getDisplayMedia({
            video: {
                displaySurface: "monitor", // 首选整个屏幕
            },
            audio: true
        })

        // 先设置状态，确保视频元素被渲染
        isClassStarted.value = true

        // 使用 nextTick 确保 DOM 已更新
        nextTick(() => {
            if (videoRef.value) {
                videoRef.value.srcObject = stream
            } else {
                console.error('视频元素不存在')
                ElMessage.error('视频元素初始化失败，请刷新页面重试')
            }
        })

        // 清除之前的所有监听器，避免重复
        socket.off('user-joined')
        socket.off('signal')

        // 设置房间监听
        setupRoomListeners()

        socket.emit('create-room', (roomId) => {
            currentRoomId.value = roomId
            classUrl.value = `${location.origin}/student/${roomId}`
            ElMessage.success('课程已开始')
        })

        // 在创建房间后注册 signal 监听器
        socket.on('signal', ({ from, signal }) => {
            console.log('收到学生信号:', from, signal)
            if (peers[from]) {
                try {
                    peers[from].signal(signal)
                } catch (err) {
                    console.error('处理信号错误:', err)
                }
            }
        })

        socket.on('user-joined', ({ id, name }) => {
            console.log('学生加入:', id, name)
            try {
                // 添加更多选项，增强兼容性
                const peer = new SimplePeer({
                    initiator: true, // 设置为发起者
                    trickle: false, // 禁用 trickle ICE
                    stream, // 共享流
                    objectMode: true, // 启用对象模式
                    sdpTransform: (sdp) => sdp // 禁用 SDP 转换
                })

                peer.on('signal', signal => {
                    console.log('发送信号给学生:', id)
                    socket.emit('signal', { to: id, from: socket.id, signal })
                })

                peer.on('error', err => {
                    console.error('Peer 错误:', err)
                })

                peers[id] = peer

                // 更新用户列表
                getRoomUsers()

                ElMessage.info(`${name || '学生'} 加入了课堂`)
            } catch (err) {
                console.error('创建 Peer 错误:', err)
            }
        })

        // 监听学生断开连接
        socket.on('disconnect', () => {
            getRoomUsers()
        })
    } catch (err) {
        console.error('获取屏幕共享失败:', err)
        ElMessage.error('获取屏幕共享失败')
    }
}

// 停止上课
function endClass() {
    ElMessageBox.confirm('确定要结束课程吗？所有学生将被断开连接。', '结束课程', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        if (currentRoomId.value) {
            socket.emit('end-class', currentRoomId.value)

            // 停止所有连接
            Object.values(peers).forEach(peer => {
                try {
                    peer.destroy()
                } catch (e) { }
            })

            // 停止屏幕共享
            if (stream) {
                stream.getTracks().forEach(track => track.stop())
            }

            // 重置状态
            peers = {}
            videoRef.value.srcObject = null
            isClassStarted.value = false
            onlineUsers.value = []

            ElMessage.success('课程已结束')
        }
    }).catch(() => { })
}

// 踢出学生
function kickStudent(studentId) {
    ElMessageBox.confirm('确定要将该学生踢出课堂吗？', '踢出学生', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        if (currentRoomId.value) {
            socket.emit('kick-student', { studentId, roomId: currentRoomId.value })

            // 销毁对应的 peer 连接
            if (peers[studentId]) {
                try {
                    peers[studentId].destroy()
                    delete peers[studentId]
                } catch (e) { }
            }

            ElMessage.success('已踢出学生')
        }
    }).catch(() => { })
}

// 组件卸载前清理资源
onBeforeUnmount(() => {
    // 停止所有连接
    Object.values(peers).forEach(peer => {
        try {
            peer.destroy()
        } catch (e) { }
    })

    // 停止屏幕共享
    if (stream) {
        stream.getTracks().forEach(track => track.stop())
    }

    // 断开 socket 连接
    socket.disconnect()
})
</script>

<style scoped>
.teacher-container {
    width: 100%;
    height: 100vh;
    display: flex;
    flex-direction: column;
    background-color: #f5f7fa;
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

.controls {
    display: flex;
    align-items: center;
    gap: 15px;
}

.status-info {
    margin-left: 10px;
    display: flex;
    align-items: center;
    color: #606266;
    gap: 5px;
}

.main-content {
    display: flex;
    flex: 1;
    padding: 20px;
    gap: 20px;
    height: calc(100vh - 70px);
    box-sizing: border-box;
}

.screen-container {
    flex: 1;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;
}

.placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
}

video {
    width: 100%;
    height: 100%;
    background-color: #000;
    object-fit: contain;
}

.video-content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #000;
    min-height: 400px;
}

.share-link {
    padding: 15px;
    border-top: 1px solid #ebeef5;
    background-color: #fff;
    z-index: 10;
}

.link-content {
    display: flex;
    align-items: center;
    margin-top: 5px;  /* 从原来的 10px 改为 5px */
}

/* 添加自定义样式覆盖 el-alert 的内部样式 */
:deep(.el-alert__title) {
    margin-bottom: -10px;
    padding-bottom: 0;
}

:deep(.el-alert__content) {
    padding: 0 8px 0 0;
}

.link-text {
    flex: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-right: 10px;
}

.students-panel {
    width: 300px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.panel-header {
    padding: 15px;
    border-bottom: 1px solid #ebeef5;
}

.panel-header h3 {
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    color: #606266;
}

.student-list {
    padding: 15px;
}

.student-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #ebeef5;
}

.student-item:last-child {
    border-bottom: none;
}

.student-info {
    display: flex;
    align-items: center;
    gap: 10px;
}

.student-name {
    font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .main-content {
        flex-direction: column;
    }

    .students-panel {
        width: 100%;
        height: 300px;
    }
}
</style>
