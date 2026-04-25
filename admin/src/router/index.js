import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layout/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/questions',
    children: [
      {
        path: 'questions',
        name: 'QuestionManage',
        component: () => import('../views/QuestionManage.vue'),
        meta: { title: '题目管理' },
      },
      {
        path: 'subjects',
        name: 'SubjectConfig',
        component: () => import('../views/SubjectConfig.vue'),
        meta: { title: '科目配置' },
      },
      {
        path: 'error-types',
        name: 'ErrorTypeConfig',
        component: () => import('../views/ErrorTypeConfig.vue'),
        meta: { title: '错因配置' },
      },
      {
        path: 'ai-config',
        name: 'AIConfig',
        component: () => import('../views/AIConfig.vue'),
        meta: { title: 'AI配置' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
