import { createRouter, createWebHistory } from 'vue-router'

import TeacherLive from '../src/views/TeacherLive.vue'
import StudentView from '../src/views/StudentView.vue'

const routes = [
    {
        path: '/',
        component: TeacherLive,
        name: 'teacher'
    },
    {
        path: '/student/:roomId',
        component: StudentView,
        name: 'student'
    },
    {
        path: '/test',
        component: () => import('../src/test/emojiTest.vue'),
        name: 'emojiTest'
    }
]

export default createRouter({
    history: createWebHistory(),
    routes
})
