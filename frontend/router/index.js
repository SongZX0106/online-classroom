import { createRouter, createWebHistory } from 'vue-router'

import TeacherLive from '../src/views/TeacherLive.vue'
import StudentView from '../src/views/StudentView.vue'

const routes = [
    {
        path: '/',
        component: TeacherLive,
        // component: () => import('../src/test/socketTest.vue'),
        name: 'teacher'
    },
    {
        path: '/student/:roomId',
        component: StudentView,
        name: 'student'
    }
]

export default createRouter({
    history: createWebHistory(),
    routes
})
