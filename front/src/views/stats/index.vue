<template>
  <div class="stats-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>学习统计</span>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="item in metrics" :key="item.label">
          <el-card shadow="never" class="metric">
            <div class="label">{{ item.label }}</div>
            <div class="value">{{ item.value }}</div>
          </el-card>
        </el-col>
      </el-row>
      <el-divider />
      <div class="section-title">薄弱点</div>
      <el-table :data="overview.weakTopics || []">
        <el-table-column prop="topic" label="主题" />
        <el-table-column prop="averageMastery" label="掌握度" width="100" />
        <el-table-column prop="weakReason" label="原因" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="header">
          <span>学习雷达</span>
          <el-button @click="loadRadar">刷新</el-button>
        </div>
      </template>
      <el-table :data="radar.dimensions || []">
        <el-table-column prop="dimension" label="分类" />
        <el-table-column prop="noteCount" label="笔记数" width="100" />
        <el-table-column prop="averageMastery" label="平均掌握度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.averageMastery" :color="masteryColor(row.averageMastery)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="weakNoteCount" label="薄弱笔记" width="100" />
        <el-table-column prop="masteredNoteCount" label="已掌握" width="100" />
      </el-table>
      <el-divider />
      <el-row :gutter="16">
        <el-col :span="6">
          <el-statistic title="总笔记数" :value="radar.totalNotes || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="总体掌握度" :value="(radar.overallMastery || 0) + '%'" />
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" class="mt16" v-if="!recommendations.insufficientData">
      <template #header>
        <div class="header">
          <span>个性化推荐</span>
          <el-button @click="loadRecommendations">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="8">
          <div class="rec-section">
            <div class="rec-title">最佳学习时段</div>
            <div class="rec-value" v-if="recommendations.bestStudyHour">
              {{ recommendations.bestStudyHour.hour }}:00 - {{ recommendations.bestStudyHour.hour + 1 }}:00
              <br /><small>{{ recommendations.bestStudyHour.sessionCount }} 次学习</small>
            </div>
            <div v-else class="rec-empty">暂无数据</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="rec-section">
            <div class="rec-title">偏好学习方式</div>
            <div class="rec-value" v-if="recommendations.preferredMode">
              {{ modeLabel(recommendations.preferredMode.activityType) }}
              <br /><small>{{ recommendations.preferredMode.count }} 次</small>
            </div>
            <div v-else class="rec-empty">暂无数据</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="rec-section">
            <div class="rec-title">推荐主题</div>
            <el-tag v-for="t in recommendations.recommendedTopics || []" :key="t.topic" class="rec-tag">
              {{ t.topic }}
            </el-tag>
            <div v-if="!(recommendations.recommendedTopics || []).length" class="rec-empty">暂无数据</div>
          </div>
        </el-col>
      </el-row>
      <el-divider />
      <div class="section-title">建议复习的笔记</div>
      <el-table :data="recommendations.suggestedNotes || []">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="masteryLevel" label="掌握度" width="100" />
        <el-table-column prop="category" label="分类" width="140" />
      </el-table>
      <el-empty v-if="!(recommendations.suggestedNotes || []).length" description="暂无建议，继续学习后将提供个性化推荐" />
    </el-card>

    <el-card shadow="never" class="mt16" v-else>
      <template #header><span>个性化推荐</span></template>
      <el-empty :description="recommendations.message || '暂无学习记录，开始学习后将提供个性化推荐。'">
        <el-button type="primary" @click="$router.push('/chat')">开始学习</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getLearningOverview, getRadar, getRecommendations } from '@/api/learn'

const overview = ref<any>({})
const radar = ref<any>({})
const recommendations = ref<any>({ insufficientData: true })

const metrics = computed(() => [
  { label: '今日学习分钟', value: overview.value.today?.studiedMinutes || 0 },
  { label: '待复习', value: overview.value.today?.dueReviewCount || 0 },
  { label: '笔记数', value: overview.value.totals?.noteCount || 0 },
  { label: '薄弱笔记', value: overview.value.totals?.weakNoteCount || 0 },
])

const masteryColor = (val: number) => {
  if (val >= 80) return '#16a34a'
  if (val >= 40) return '#2563eb'
  return '#dc2626'
}

const modeLabel = (type: string) => {
  const map: Record<string, string> = {
    quiz_answer: '测验练习',
    flashcard_review: '闪卡复习',
    note_create: '创建笔记',
    document_import: '导入资料',
    mastery_update: '掌握度更新',
    quiz_generate: '生成测验',
  }
  return map[type] || type
}

const load = async () => { overview.value = await getLearningOverview() }
const loadRadar = async () => { radar.value = await getRadar() }
const loadRecommendations = async () => { recommendations.value = await getRecommendations() }

onMounted(() => {
  load()
  loadRadar()
  loadRecommendations()
})
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; }
.mt16 { margin-top: 16px; }
.metric { text-align: center; }
.label { color: #909399; }
.value { margin-top: 8px; font-size: 28px; font-weight: 700; }
.section-title { font-weight: 700; margin-bottom: 12px; }
.rec-section { min-height: 90px; padding: 12px; border: 1px solid #ebeef5; border-radius: 12px; background: #fafafa; }
.rec-title { font-weight: 700; margin-bottom: 8px; color: #1f2937; }
.rec-value { line-height: 1.7; }
.rec-empty { color: #909399; margin-top: 8px; }
.rec-tag { margin-right: 6px; margin-bottom: 6px; }
</style>
