import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { ElMessage } from 'element-plus'
import Layout from '@/layouts/BasicLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录', requiresAuth: false } },
    { path: '/register', name: 'Register', component: () => import('@/views/register/index.vue'), meta: { title: '注册', requiresAuth: false } },
    { path: '/init-admin', name: 'InitAdmin', component: () => import('@/views/init-admin/index.vue'), meta: { title: '初始化管理员', requiresAuth: false } },
    {
      path: '/',
      component: Layout,
      redirect: '/home',
      children: [
        { path: 'home', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页', requiresAuth: true } },
        { path: 'demo', name: 'Demo', component: () => import('@/views/demo/index.vue'), meta: { title: '演示中心', requiresAuth: true } },
        { path: 'deploy', name: 'Deploy', component: () => import('@/views/deploy/index.vue'), meta: { title: '部署验收', requiresAuth: true } },
        { path: 'release', name: 'Release', component: () => import('@/views/release/index.vue'), meta: { title: '发布说明', requiresAuth: true } },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '仪表盘', requiresAuth: true } },
        { path: 'docs', name: 'Docs', component: () => import('@/views/docs/index.vue'), meta: { title: '接口文档', requiresAuth: true } },
        { path: 'chat', name: 'Chat', component: () => import('@/views/learn-chat/index.vue'), meta: { title: 'ReAct 学习对话', requiresAuth: true } },
        { path: 'notes', name: 'Notes', component: () => import('@/views/notes/index.vue'), meta: { title: '知识笔记', requiresAuth: true } },
        { path: 'quiz', name: 'Quiz', component: () => import('@/views/quiz/index.vue'), meta: { title: '测验练习', requiresAuth: true } },
        { path: 'flashcards', name: 'Flashcards', component: () => import('@/views/flashcards/index.vue'), meta: { title: '闪卡复习', requiresAuth: true } },
        { path: 'graph', name: 'Graph', component: () => import('@/views/graph/index.vue'), meta: { title: '知识图谱', requiresAuth: true } },
        { path: 'paths', name: 'Paths', component: () => import('@/views/paths/index.vue'), meta: { title: '学习路径', requiresAuth: true } },
        { path: 'stats', name: 'Stats', component: () => import('@/views/stats/index.vue'), meta: { title: '学习统计', requiresAuth: true } },
        { path: 'agent', name: 'Agent', component: () => import('@/views/agent/index.vue'), meta: { title: 'Agent 管理', requiresAuth: true } },
        { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/knowledge/index.vue'), meta: { title: '知识库', requiresAuth: true } },
        { path: 'planning', name: 'Planning', component: () => import('@/views/planning/index.vue'), meta: { title: '任务规划', requiresAuth: true } },
        { path: 'stream', name: 'Stream', component: () => import('@/views/stream/index.vue'), meta: { title: '基础对话', requiresAuth: true } },
        { path: 'session', name: 'Session', component: () => import('@/views/session/index.vue'), meta: { title: '会话管理', requiresAuth: true } },
        { path: 'monitor', name: 'Monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: '监控面板', requiresAuth: true } },
        { path: 'audit', name: 'Audit', component: () => import('@/views/audit/index.vue'), meta: { title: '审计日志', requiresAuth: true } },
        { path: 'admin', name: 'Admin', component: () => import('@/views/admin/index.vue'), meta: { title: '用户管理', requiresAuth: true } },
      ],
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  const hasToken = Boolean(userStore.token)

  if (to.meta.title) document.title = `${to.meta.title} - AI Agent 管理台`

  if (to.meta.requiresAuth && !hasToken) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }

  if (hasToken && !permissionStore.loaded) {
    try {
      await permissionStore.loadPermission()
    } catch {
      userStore.clearUser()
      permissionStore.clearPermission()
      next('/login')
      return
    }
  }

  if (hasToken && permissionStore.loaded && to.meta.requiresAuth) {
    const routeKey = String(to.name || '').toLowerCase()
    if (routeKey && permissionStore.menus.length > 0 && !permissionStore.hasMenu(routeKey)) {
      ElMessage.warning('当前角色无权访问该页面')
      next('/home')
      return
    }
  }

  if ((to.path === '/login' || to.path === '/register' || to.path === '/init-admin') && hasToken) {
    next('/home')
    return
  }

  next()
})

export default router
