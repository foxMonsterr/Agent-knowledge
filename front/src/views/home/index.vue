<template>
  <div class="home-page">
    <section class="studio-hero">
      <div class="hero-copy">
        <div class="hero-kicker">Personal Knowledge OS</div>
        <p>把资料、对话和复习压进同一个学习闭环。</p>
        <p>从知识库提问开始，沿着 ReAct 链路沉淀笔记、生成测验、安排闪卡，最后回到下一轮学习。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/chat')">开始 ReAct 学习</el-button>
        <el-button size="large" @click="$router.push('/knowledge')">导入资料</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card" :class="metric.tone">
        <div class="metric-topline">
          <div class="metric-label">{{ metric.label }}</div>
          <div class="metric-index">{{ metric.index }}</div>
        </div>
        <div class="metric-value">{{ metric.value }}</div>
        <div class="metric-subtitle">{{ metric.subtitle }}</div>
      </article>
    </section>

    <el-row :gutter="16" class="mt16">
      <el-col :span="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="header">
              <span>最近学习主题</span>
              <el-button type="primary" plain @click="$router.push('/chat')">开始 ReAct 学习</el-button>
            </div>
          </template>
          <el-table :data="overview.recentTopics || []" height="320">
            <el-table-column prop="topic" label="主题" />
            <el-table-column prop="category" label="分类" width="140" />
            <el-table-column prop="averageMastery" label="掌握度" width="100" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="header">
              <span>薄弱点</span>
              <el-button plain @click="$router.push('/quiz')">去练习</el-button>
            </div>
          </template>
          <el-empty v-if="!(overview.weakTopics || []).length" description="暂无薄弱点，继续学习后会自动分析" />
          <div v-for="item in overview.weakTopics || []" :key="item.topic" class="weak-item">
            <div class="weak-title">{{ item.topic }}</div>
            <div class="weak-desc">{{ item.weakReason || `当前掌握度 ${item.averageMastery}` }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :span="24">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="header">
              <span>下一步行动</span>
              <el-button @click="load">刷新</el-button>
            </div>
          </template>
          <div class="action-grid">
            <button v-for="action in quickActions" :key="action.path" class="action-card" @click="$router.push(action.path)">
              <span class="action-title">{{ action.label }}</span>
              <span class="action-desc">{{ action.desc }}</span>
            </button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getLearningOverview } from '@/api/learn'

const overview = ref<any>({})

const metrics = computed(() => [
  { index: '01', label: '今日学习', value: overview.value.today?.studiedMinutes || 0, subtitle: '分钟', tone: 'tone-study' },
  { index: '02', label: '待复习', value: overview.value.today?.dueReviewCount || 0, subtitle: '张闪卡', tone: 'tone-review' },
  { index: '03', label: '知识笔记', value: overview.value.totals?.noteCount || 0, subtitle: '条', tone: 'tone-note' },
  { index: '04', label: '薄弱笔记', value: overview.value.totals?.weakNoteCount || 0, subtitle: '条', tone: 'tone-weak' },
])

const quickActions = [
  { label: 'ReAct 对话', desc: '带来源和链路的学习问答', path: '/chat' },
  { label: '创建笔记', desc: '沉淀概念、例子和错题', path: '/notes' },
  { label: '导入资料', desc: '上传文档并进入知识库', path: '/knowledge' },
  { label: '学习路径', desc: 'AI 规划分阶段学习', path: '/paths' },
  { label: '复习闪卡', desc: '按间隔复习刷新记忆', path: '/flashcards' },
  { label: '测验练习', desc: '生成题目检验掌握度', path: '/quiz' },
  { label: '查看图谱', desc: '从关系网络里找下一站', path: '/graph' },
]

const load = async () => {
  overview.value = await getLearningOverview()
}

onMounted(load)
</script>

<style scoped>
.home-page {
  padding: 2px;
}

.studio-hero {
  position: relative;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: end;
  min-height: 220px;
  padding: 28px;
  border: 1px solid rgba(190, 211, 202, 0.72);
  border-radius: 26px;
  background:
    radial-gradient(circle at 88% 16%, rgba(15, 118, 110, 0.2), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(234, 246, 241, 0.88));
  box-shadow: var(--la-shadow-soft);
}

.studio-hero::after {
  content: '';
  position: absolute;
  right: 28px;
  top: 28px;
  width: 120px;
  height: 120px;
  border-radius: 38px;
  border: 1px solid rgba(15, 118, 110, 0.16);
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.14), transparent),
    repeating-linear-gradient(135deg, rgba(15, 118, 110, 0.12) 0 1px, transparent 1px 12px);
  transform: rotate(8deg);
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 760px;
}

.hero-kicker {
  margin-bottom: 10px;
  color: var(--la-accent-strong);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  max-width: 760px;
  font-size: clamp(30px, 4vw, 54px);
  line-height: 1.04;
  letter-spacing: -0.06em;
  color: var(--la-ink);
}

.hero-copy p {
  max-width: 58ch;
  margin-top: 14px;
  color: var(--la-ink-muted);
  line-height: 1.8;
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  white-space: nowrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.mt16 { margin-top: 16px; }

.metric-card,
.panel-card {
  border-radius: 18px;
}

.metric-card {
  min-height: 138px;
  padding: 18px;
  border: 1px solid rgba(190, 211, 202, 0.72);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--la-shadow-soft);
}

.metric-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.metric-label {
  color: var(--la-ink-muted);
  font-weight: 750;
}

.metric-index {
  color: rgba(15, 118, 110, 0.42);
  font-size: 12px;
  font-weight: 900;
}

.metric-value {
  margin-top: 16px;
  font-size: 38px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.06em;
  color: var(--la-ink);
}

.metric-subtitle {
  margin-top: 8px;
  color: var(--la-ink-muted);
}

.tone-study { background: linear-gradient(135deg, #ffffff, #eef4ff); }
.tone-review { background: linear-gradient(135deg, #ffffff, #eef9f6); }
.tone-note { background: linear-gradient(135deg, #ffffff, #f5faf0); }
.tone-weak { background: linear-gradient(135deg, #ffffff, #fff5f2); }

.header { display: flex; align-items: center; justify-content: space-between; }

.weak-item {
  border: 1px solid rgba(190, 211, 202, 0.72);
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 10px;
  background: #fff;
}

.weak-title {
  font-weight: 800;
  color: #b54b3b;
}

.weak-desc {
  margin-top: 6px;
  color: var(--la-ink-muted);
  line-height: 1.6;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 110px;
  padding: 16px;
  border: 1px solid rgba(190, 211, 202, 0.72);
  border-radius: 18px;
  text-align: left;
  color: var(--la-ink);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(244, 250, 247, 0.92));
  cursor: pointer;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 118, 110, 0.38);
  box-shadow: var(--la-shadow-soft);
}

.action-title {
  font-weight: 850;
}

.action-desc {
  color: var(--la-ink-muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1200px) {
  .metric-grid,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .studio-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    flex-wrap: wrap;
  }
}
</style>
