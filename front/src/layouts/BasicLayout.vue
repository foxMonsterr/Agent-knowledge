<template>
  <el-container class="layout-shell">
    <el-aside class="layout-aside" width="230px">
      <div class="brand">
        <div class="brand-logo">
          <span>LA</span>
        </div>
        <div class="brand-text">
          <div class="brand-title">LearnAgent</div>
          <div class="brand-subtitle">Knowledge ReAct OS</div>
        </div>
      </div>

      <div class="aside-status">
        <div>
          <div class="aside-status-label">当前工作区</div>
          <div class="aside-status-value">{{ activeGroupLabel }}</div>
        </div>
        <span class="pulse-dot" />
      </div>

      <el-menu
        router
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        class="side-menu"
        :collapse-transition="false"
        unique-opened
      >
        <el-sub-menu v-for="group in visibleMenuGroups" :key="group.key" :index="group.key">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.label }}</span>
          </template>
          <el-menu-item v-for="item in group.children" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <div class="eyebrow">{{ activeGroupLabel }}</div>
          <div class="page-title">{{ pageTitle }}</div>
          <div class="page-desc">{{ pageDescription }}</div>
        </div>

        <div class="header-right">
          <el-tag type="success" effect="plain">SSE + Memory</el-tag>
          <el-tag type="info" effect="plain">{{ userStore.role || 'GUEST' }}</el-tag>
          <el-dropdown trigger="click">
            <span class="user-chip">
              {{ userStore.nickname || userStore.username || '未登录' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ userStore.username || '未登录' }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  ChatDotRound,
  Collection,
  Cpu,
  Document,
  EditPen,
  Files,
  House,
  Monitor,
  Notebook,
  Setting,
  Share,
  TrendCharts,
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const pageTitle = computed(() => (route.meta.title as string) || 'LearnAgent')

const pageDescription = computed(() => {
  if (route.path.startsWith('/chat')) return '学习问题、知识检索和 ReAct 链路集中在这里'
  if (route.path.startsWith('/agent')) return '通用工具 Agent，可切换普通流式和 ReAct 模式'
  if (route.path.startsWith('/knowledge')) return '资料导入、RAG 检索和知识库问答'
  if (route.path.startsWith('/planning')) return '复杂任务拆解、执行和流式过程观察'
  if (route.path.startsWith('/session')) return '统一 Conversation 历史、Memory 和上下文管理'
  return '个人知识库、学习闭环和 Agent 能力控制台'
})

interface MenuLeaf {
  path: string
  label: string
  key: string
  icon: Component
}

interface MenuGroup {
  key: string
  label: string
  icon: Component
  children: MenuLeaf[]
}

const menuGroups: MenuGroup[] = [
  {
    key: 'learn-core',
    label: '学习中心',
    icon: House,
    children: [
      { path: '/home', label: '学习工作台', key: 'home', icon: House },
      { path: '/chat', label: 'ReAct 对话', key: 'chat', icon: ChatDotRound },
      { path: '/notes', label: '知识笔记', key: 'notes', icon: Notebook },
      { path: '/knowledge', label: '资料库', key: 'knowledge', icon: Files },
    ],
  },
  {
    key: 'learn-training',
    label: '知识训练',
    icon: Collection,
    children: [
      { path: '/quiz', label: '测验练习', key: 'quiz', icon: EditPen },
      { path: '/flashcards', label: '闪卡复习', key: 'flashcards', icon: Collection },
      { path: '/graph', label: '知识图谱', key: 'graph', icon: Share },
      { path: '/paths', label: '学习路径', key: 'paths', icon: Share },
      { path: '/stats', label: '学习统计', key: 'stats', icon: TrendCharts },
      { path: '/session', label: '学习会话', key: 'session', icon: ChatDotRound },
    ],
  },
  {
    key: 'agent-capability',
    label: 'Agent 能力',
    icon: Cpu,
    children: [
      { path: '/agent', label: 'Agent 管理', key: 'agent', icon: Cpu },
      { path: '/planning', label: '任务规划', key: 'planning', icon: Share },
      { path: '/stream', label: '基础对话', key: 'stream', icon: ChatDotRound },
    ],
  },
  {
    key: 'system-admin',
    label: '系统管理',
    icon: Setting,
    children: [
      { path: '/monitor', label: '监控面板', key: 'monitor', icon: Monitor },
      { path: '/audit', label: '审计日志', key: 'audit', icon: Document },
      { path: '/admin', label: '用户管理', key: 'admin', icon: Setting },
    ],
  },
  {
    key: 'project-helper',
    label: '项目辅助',
    icon: Document,
    children: [
      { path: '/dashboard', label: '仪表盘', key: 'dashboard', icon: TrendCharts },
      { path: '/demo', label: '演示中心', key: 'demo', icon: Monitor },
      { path: '/docs', label: '接口文档', key: 'docs', icon: Document },
      { path: '/deploy', label: '部署验收', key: 'deploy', icon: Files },
      { path: '/release', label: '发布说明', key: 'release', icon: Document },
    ],
  },
]

const canDisplayMenu = (key: string) => !permissionStore.loaded || permissionStore.hasMenu(key)

const visibleMenuGroups = computed(() =>
  menuGroups
    .map(group => ({
      ...group,
      children: group.children.filter(item => canDisplayMenu(item.key)),
    }))
    .filter(group => group.children.length > 0),
)

const activeMenu = computed(() => route.path)

const defaultOpeneds = computed(() => {
  const activeGroup = visibleMenuGroups.value.find(group =>
    group.children.some(item => item.path === route.path),
  )
  return activeGroup ? [activeGroup.key] : visibleMenuGroups.value.slice(0, 1).map(group => group.key)
})

const activeGroupLabel = computed(() => {
  const activeGroup = visibleMenuGroups.value.find(group =>
    group.children.some(item => item.path === route.path),
  )
  return activeGroup?.label || '学习中心'
})

const handleLogout = async () => {
  await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  userStore.clearUser()
  permissionStore.clearPermission()
  router.push('/login')
}

onMounted(async () => {
  if (userStore.token && !permissionStore.loaded) {
    await permissionStore.loadPermission()
  }
})
</script>

<style scoped lang="scss">
.layout-shell {
  height: 100%;
  min-height: 100dvh;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.38), transparent 34%),
    var(--la-bg);
}

.layout-aside {
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(190, 211, 202, 0.72);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 251, 248, 0.9)),
    var(--la-surface-solid);
  box-shadow: 12px 0 40px rgba(16, 44, 39, 0.06);
}

.layout-aside::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 180px;
  background: radial-gradient(circle at 30% 0%, rgba(15, 118, 110, 0.18), transparent 62%);
  pointer-events: none;
}

.brand {
  position: relative;
  z-index: 1;
  height: 78px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 900;
  letter-spacing: -0.04em;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.28), transparent 34%),
    linear-gradient(135deg, #0f766e, #143f39);
  box-shadow: 0 16px 32px rgba(15, 118, 110, 0.26);
}

.brand-title {
  font-size: 17px;
  font-weight: 850;
  letter-spacing: -0.03em;
  color: var(--la-ink);
}

.brand-subtitle {
  margin-top: 3px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--la-ink-muted);
}

.aside-status {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 14px 12px;
  padding: 13px 14px;
  border: 1px solid rgba(190, 211, 202, 0.8);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 12px 28px rgba(16, 44, 39, 0.06);
}

.aside-status-label {
  font-size: 11px;
  color: var(--la-ink-muted);
}

.aside-status-value {
  margin-top: 3px;
  font-size: 14px;
  font-weight: 800;
  color: var(--la-ink);
}

.pulse-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--la-accent);
  box-shadow: 0 0 0 6px rgba(15, 118, 110, 0.12);
}

.side-menu {
  position: relative;
  z-index: 1;
  height: calc(100% - 150px);
  border-right: none;
  overflow-y: auto;
  padding: 4px 10px 14px;
  background: transparent;
}

.side-menu :deep(.el-sub-menu__title) {
  height: 42px;
  margin: 3px 0;
  border-radius: 13px;
  font-weight: 800;
  color: #28453f;
}

.side-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(15, 118, 110, 0.08);
}

.side-menu :deep(.el-menu-item) {
  height: 38px;
  margin: 3px 0;
  border-radius: 12px;
  color: #52665f;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  min-width: 0;
  padding-left: 42px !important;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(15, 118, 110, 0.07);
}

.side-menu :deep(.el-menu-item.is-active) {
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.16), rgba(15, 118, 110, 0.08));
  color: var(--la-accent-strong);
  font-weight: 850;
  box-shadow: inset 0 0 0 1px rgba(15, 118, 110, 0.14);
}

.layout-header {
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 14px 18px 0;
  padding: 0 20px;
  border: 1px solid rgba(190, 211, 202, 0.72);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
  box-shadow: var(--la-shadow-soft);
}

.eyebrow {
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.08em;
  color: var(--la-accent-strong);
}

.page-title {
  margin-top: 2px;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.04em;
  color: var(--la-ink);
}

.page-desc {
  font-size: 12px;
  color: var(--la-ink-muted);
  margin-top: 2px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  color: #28453f;
  cursor: pointer;
  background: rgba(15, 118, 110, 0.08);
  border: 1px solid rgba(15, 118, 110, 0.12);
}

.layout-main {
  min-width: 0;
  padding: 18px;
}

@media (max-width: 980px) {
  .layout-aside {
    width: 72px !important;
  }

  .brand-text,
  .aside-status,
  .side-menu :deep(.el-sub-menu__title span),
  .side-menu :deep(.el-menu-item span) {
    display: none;
  }

  .brand {
    justify-content: center;
    padding: 0;
  }

  .side-menu :deep(.el-sub-menu__title),
  .side-menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }
}
</style>
