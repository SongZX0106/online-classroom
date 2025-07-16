// backend/server.js
const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');
const { v4: uuidv4 } = require('uuid');

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
    cors: { origin: '*' }
});

// 存储房间信息，包括用户列表和老师ID
const rooms = {};
// 存储用户信息，包括用户名和角色
const users = {};

io.on('connection', (socket) => {
    console.log('client connected:', socket.id);

    // 创建房间
    socket.on('create-room', (cb) => {
        const roomId = uuidv4();
        // 记录房间信息，包括老师ID
        rooms[roomId] = {
            users: [socket.id],
            teacher: socket.id
        };
        users[socket.id] = {
            name: '老师',
            role: 'teacher',
            room: roomId
        };
        socket.join(roomId);
        cb(roomId);
    });

    // 加入房间
    socket.on('join-room', ({ roomId, userName = '学生' }) => {
        if (rooms[roomId]) {
            // 记录用户信息
            users[socket.id] = {
                name: userName,
                role: 'student',
                room: roomId
            };

            // 添加到房间
            rooms[roomId].users.push(socket.id);
            socket.join(roomId);

            // 通知老师有新学生加入
            socket.to(roomId).emit('user-joined', { id: socket.id, name: userName });

            // 通知房间内所有人更新人数
            io.to(roomId).emit('room-users', getRoomUsers(roomId));
        }
    });

    // 信令转发
    socket.on('signal', ({ to, from, signal }) => {
        io.to(to).emit('signal', { from, signal });
    });

    // 获取房间用户列表
    socket.on('get-room-users', (roomId, callback) => {
        if (rooms[roomId]) {
            callback(getRoomUsers(roomId));
        } else {
            callback([]);
        }
    });

    // 踢出学生
    socket.on('kick-student', ({ studentId, roomId }) => {
        const room = rooms[roomId];

        // 验证请求者是否为老师
        if (room && room.teacher === socket.id) {
            // 通知被踢学生
            io.to(studentId).emit('kicked');

            // 从房间中移除
            if (users[studentId]) {
                const userSocket = io.sockets.sockets.get(studentId);
                if (userSocket) {
                    userSocket.leave(roomId);
                }

                // 更新房间用户列表
                room.users = room.users.filter(id => id !== studentId);
                delete users[studentId];

                // 通知房间内所有人更新人数
                io.to(roomId).emit('room-users', getRoomUsers(roomId));
            }
        }
    });

    // 结束课程
    socket.on('end-class', (roomId) => {
        const room = rooms[roomId];

        // 验证请求者是否为老师
        if (room && room.teacher === socket.id) {
            // 通知所有学生课程结束
            io.to(roomId).emit('class-ended');

            // 清理房间
            delete rooms[roomId];

            // 清理用户信息
            room.users.forEach(userId => {
                if (users[userId]) {
                    delete users[userId];
                }
            });
        }
    });

    // 断开连接处理
    socket.on('disconnect', () => {
        const user = users[socket.id];
        if (user) {
            const roomId = user.room;
            const room = rooms[roomId];

            if (room) {
                // 从房间中移除用户
                room.users = room.users.filter(id => id !== socket.id);

                // 如果是老师断开，通知所有学生课程结束
                if (user.role === 'teacher') {
                    io.to(roomId).emit('class-ended');
                    delete rooms[roomId];
                } else {
                    // 通知房间内所有人更新人数
                    io.to(roomId).emit('room-users', getRoomUsers(roomId));
                }
            }

            delete users[socket.id];
        }
    });
});

// 获取房间用户列表
function getRoomUsers(roomId) {
    const room = rooms[roomId];
    if (!room) return [];

    return room.users.map(userId => {
        const user = users[userId];
        return {
            id: userId,
            name: user ? user.name : '未知用户',
            role: user ? user.role : 'student'
        };
    });
}

httpServer.listen(3000, () => {
    console.log('🚀 Server listening on http://localhost:3000');
});
