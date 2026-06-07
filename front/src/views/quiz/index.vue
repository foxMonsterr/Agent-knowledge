<template>
  <el-card shadow="never">
    <template #header>
      <div class="header">
        <span>测验练习</span>
        <el-button @click="loadNotes">刷新笔记</el-button>
      </div>
    </template>
    <div class="toolbar">
      <el-select v-model="selectedNoteId" placeholder="选择笔记" filterable>
        <el-option v-for="note in notes" :key="note.noteId" :label="note.title" :value="note.noteId" />
      </el-select>
      <el-input-number v-model="count" :min="1" :max="20" controls-position="right" style="width: 140px" />
      <el-button type="primary" @click="handleGenerate">生成测验</el-button>
    </div>
    <el-divider />
    <div v-for="item in quizzes" :key="item.quizId" class="quiz-card">
      <div class="question">{{ item.question }}</div>
      <div class="answer-area">
        <el-radio-group v-if="item.options?.length" v-model="answers[item.quizId]">
          <el-radio v-for="option in item.options" :key="option.key" :label="option.key">{{ option.key }}. {{ option.text }}</el-radio>
        </el-radio-group>
        <el-input v-else v-model="answers[item.quizId]" placeholder="输入你的答案" />
      </div>
      <div class="quiz-footer">
        <el-button type="primary" :icon="Check" @click="handleEvaluate(item.quizId)">提交答案</el-button>
      </div>
      <el-alert v-if="results[item.quizId]" class="mt8" type="success" :title="JSON.stringify(results[item.quizId])" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { evaluateQuiz, generateQuiz, listNotes } from '@/api/learn'
import type { KnowledgeNote, QuizItem } from '@/types/learn'

const notes = ref<KnowledgeNote[]>([])
const selectedNoteId = ref('')
const count = ref(5)
const quizzes = ref<QuizItem[]>([])
const answers = reactive<Record<string, string>>({})
const results = reactive<Record<string, any>>({})

const loadNotes = async () => { notes.value = await listNotes() }
const handleGenerate = async () => {
  if (!selectedNoteId.value) return ElMessage.warning('请选择笔记')
  const res = await generateQuiz(selectedNoteId.value, count.value)
  quizzes.value = res.items
}
const handleEvaluate = async (quizId: string) => {
  results[quizId] = await evaluateQuiz(quizId, answers[quizId] || '')
}
onMounted(loadNotes)
</script>

<style scoped>
.header, .toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.toolbar .el-select { flex: 1; }
.quiz-card { padding: 14px; border: 1px solid #ebeef5; border-radius: 8px; margin-bottom: 12px; }
.question { font-weight: 700; margin-bottom: 10px; }
.answer-area { margin-bottom: 14px; }
.quiz-footer { display: flex; justify-content: flex-end; padding-top: 10px; border-top: 1px solid #ebeef5; }
.mt8 { margin-top: 8px; }
</style>
