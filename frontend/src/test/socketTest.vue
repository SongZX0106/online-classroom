<template>
    <div>测试</div>
</template>
<script setup>
import { io } from 'socket.io-client'
import { onMounted } from 'vue';

const socket = io('http://localhost:3000')

onMounted(() => {
    socket.emit("create-room", "66666", (roomId) => {
        console.log("创建房间成功", roomId)
    })
})
</script>
