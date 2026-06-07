<template>
  <div class="paths-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="header">
              <span>学习路径</span>
              <el-button type="primary" size="small" @click="showCreate = true">新建路径</el-button>
            </div>
          </template>
          <el-empty v-if="!paths.length" description="暂无学习路径，点击「新建路径」开始规划" />
          <div v-for="path in paths" :key="path.pathId" class="path-item" :class="{ active: selectedPathId === path.pathId }" @click="selectPath(path.pathId)">
            <div class="path-topic">{{ path.topic }}</div>
            <div class="path-meta">
              <el-tag :type="path.status === 'completed' ? 'success' : path.status === 'active' ? 'primary' : 'info'" size="small">
                {{ path.status === 'completed' ? '已完成' : path.status === 'active' ? '进行中' : path.status }}
              </el-tag>
              <span class="path-progress-text">{{ path.completedStages }}/{{ path.totalStages }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" v-if="progress">
          <template #header>
            <div class="header">
              <span>{{ progress.topic }}</span>
              <el-tag :type="progress.status === 'completed' ? 'success' : 'primary'">
                {{ progress.status === 'completed' ? '已完成' : '进行中' }}
              </el-tag>
            </div>
          </template>
          <div class="progress-bar-section">
            <el-progress :percentage="progress.progress" :stroke-width="14" :color="'#0f766e'" />
            <div class="progress-text">{{ progress.completedStages }} / {{ progress.totalStages }} 阶段</div>
          </div>
          <el-divider />
          <div class="stages">
            <div v-for="stage in progress.stages" :key="stage.stageId" class="stage-item">
              <div class="stage-order">
                <el-tag :type="stage.status === 'completed' ? 'success' : 'info'" size="small" effect="dark">
                  {{ stage.order + 1 }}
                </el-tag>
              </div>
              <div class="stage-body">
                <div class="stage-title">{{ stage.title }}</div>
                <div class="stage-desc">{{ stage.description }}</div>
                <div class="stage-meta">
                  <el-tag size="small">{{ actionLabel(stage.action) }}</el-tag>
                  <span v-if="stage.score > 0" class="stage-score">得分: {{ stage.score }}</span>
                  <span v-if="stage.completedAt" class="stage-time">{{ stage.completedAt }}</span>
                </div>
              </div>
              <div class="stage-action" v-if="stage.status !== 'completed'">
                <el-button size="small" type="primary" @click="handleComplete(stage.stageId)">完成</el-button>
              </div>
            </div>
          </div>
        </el-card>
        <el-empty v-else description="选择左侧的学习路径查看详情" />
      </el-col>
    </el-row>

    <el-dialog v-model="showCreate" title="新建学习路径" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="主题">
          <el-input v-model="createForm.topic" placeholder="例如：机器学习基础" />
        </el-form-item>
        <el-form-item label="目标笔记">
          <el-input-number v-model="createForm.targetNoteCount" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="深度">
          <el-select v-model="createForm.preferredDepth">
            <el-option label="入门" value="beginner" />
            <el-option label="中级" value="intermediate" />
            <el-option label="高级" value="advanced" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createPath, getPathProgress, listPaths, updatePathStage } from '@/api/learn'

const paths = ref<any[]>([])
const selectedPathId = ref<string | null>(null)
const progress = ref<any>(null)
const showCreate = ref(false)
const creating = ref(false)

const createForm = reactive({
  topic: '',
  targetNoteCount: 5,
  preferredDepth: 'intermediate',
})

const actionLabel = (action: string) => {
  const map: Record<string, string> = {
    review: '复习笔记',
    quiz: '做题测验',
    create: '创建笔记',
    flashcard: '制作闪卡',
  }
  return map[action] || action
}

const loadPaths = async () => {
  const data = await listPaths()
  paths.value = (Array.isArray(data) ? data : (data as any).data) || []
}

const selectPath = async (pathId: string) => {
  selectedPathId.value = pathId
  progress.value = await getPathProgress(pathId)
}

const handleComplete = async (stageId: string) => {
  if (!selectedPathId.value) return
  await updatePathStage(selectedPathId.value, { stageId, status: 'completed' })
  ElMessage.success('阶段已完成')
  await Promise.all([loadPaths(), selectPath(selectedPathId.value!)])
}

const handleCreate = async () => {
  if (!createForm.topic.trim()) {
    ElMessage.warning('请输入学习主题')
    return
  }
  creating.value = true
  try {
    const result: any = await createPath({
      topic: createForm.topic,
      targetNoteCount: createForm.targetNoteCount,
      preferredDepth: createForm.preferredDepth,
    })
    showCreate.value = false
    createForm.topic = ''
    ElMessage.success('学习路径创建成功')
    await loadPaths()
    const pathId = result.pathId || result.data?.pathId
    if (pathId) await selectPath(pathId)
  } catch (err: any) {
    ElMessage.error(err?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

onMounted(loadPaths)
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; }
.path-item { border: 1px solid #ebeef5; border-radius: 12px; padding: 14px; margin-bottom: 10px; cursor: pointer; transition: all 0.2s; }
.path-item:hover { border-color: #0f766e; }
.path-item.active { border-color: #0f766e; background: rgba(15, 118, 110, 0.06); }
.path-topic { font-weight: 700; margin-bottom: 8px; }
.path-meta { display: flex; align-items: center; gap: 10px; }
.path-progress-text { color: #909399; font-size: 13px; }
.progress-bar-section { padding: 8px 0; }
.progress-text { margin-top: 8px; color: #909399; font-size: 13px; }
.stages { margin-top: 8px; }
.stage-item { display: flex; align-items: flex-start; gap: 14px; padding: 14px; border: 1px solid #ebeef5; border-radius: 12px; margin-bottom: 10px; }
.stage-order { padding-top: 2px; }
.stage-body { flex: 1; min-width: 0; }
.stage-title { font-weight: 700; }
.stage-desc { margin-top: 4px; color: #606266; line-height: 1.6; font-size: 13px; }
.stage-meta { margin-top: 8px; display: flex; align-items: center; gap: 10px; font-size: 12px; color: #909399; }
.stage-score { font-weight: 700; color: #0f766e; }
.stage-action { flex-shrink: 0; }
</style>
