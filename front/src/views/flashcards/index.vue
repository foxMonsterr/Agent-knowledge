<template>
  <div class="flashcards-page">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>闪卡复习</span>
          <el-button @click="loadAll">刷新</el-button>
        </div>
      </template>
      <div class="toolbar">
        <el-select v-model="selectedNoteId" placeholder="选择笔记生成闪卡" filterable>
          <el-option v-for="note in notes" :key="note.noteId" :label="note.title" :value="note.noteId" />
        </el-select>
        <el-button type="primary" @click="handleGenerate">生成闪卡</el-button>
      </div>
      <el-divider />
      <el-empty v-if="!cards.length" description="暂无到期闪卡" />
      <div v-for="card in cards" :key="card.cardId" class="card">
        <div class="front">{{ card.front }}</div>
        <div class="back">{{ card.back }}</div>
        <div class="score-row">
          <el-button v-for="quality in [0,1,2,3,4,5]" :key="quality" size="small" @click="handleReview(card.cardId, quality)">
            {{ quality }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="header">
          <span>优先复习队列</span>
          <el-button @click="loadPriorityQueue">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="16" v-if="queueStats">
        <el-col :span="6">
          <el-statistic title="总计到期" :value="queueStats.totalDue || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="高优先级" :value="queueStats.highPriority || 0">
            <template #suffix><el-tag type="danger" size="small">立即复习</el-tag></template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="中优先级" :value="queueStats.mediumPriority || 0" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="低优先级" :value="queueStats.lowPriority || 0" />
        </el-col>
      </el-row>
      <el-divider />
      <el-table :data="priorityItems" max-height="400">
        <el-table-column prop="front" label="正面" />
        <el-table-column prop="back" label="背面" width="200" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="row.priority >= 80 ? 'danger' : row.priority >= 50 ? 'warning' : 'info'" size="small">
              {{ row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" width="100" />
        <el-table-column prop="masteryLevel" label="掌握度" width="100" />
      </el-table>
      <el-empty v-if="!priorityItems.length" description="暂无待复习闪卡" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateFlashcards, getPriorityQueue, listDueFlashcards, listNotes, reviewFlashcard } from '@/api/learn'
import type { Flashcard, KnowledgeNote } from '@/types/learn'

const notes = ref<KnowledgeNote[]>([])
const cards = ref<Flashcard[]>([])
const selectedNoteId = ref('')
const queueStats = ref<any>(null)

const priorityItems = computed(() => queueStats.value?.items || [])

const loadAll = async () => {
  notes.value = await listNotes()
  cards.value = await listDueFlashcards()
}
const loadPriorityQueue = async () => {
  queueStats.value = await getPriorityQueue()
}
const handleGenerate = async () => {
  if (!selectedNoteId.value) return ElMessage.warning('请选择笔记')
  await generateFlashcards(selectedNoteId.value, 10)
  ElMessage.success('闪卡已生成，明天开始复习')
  await loadAll()
}
const handleReview = async (cardId: string, quality: number) => {
  await reviewFlashcard(cardId, quality)
  ElMessage.success('复习结果已保存')
  await Promise.all([loadAll(), loadPriorityQueue()])
}
onMounted(() => {
  loadAll()
  loadPriorityQueue()
})
</script>

<style scoped>
.header, .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.mt16 { margin-top: 16px; }
.toolbar .el-select { flex: 1; }
.card { border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; margin-bottom: 12px; background: #fff; }
.front { font-size: 18px; font-weight: 700; }
.back { margin-top: 10px; color: #606266; line-height: 1.8; }
.score-row { margin-top: 12px; display: flex; gap: 8px; }
</style>
