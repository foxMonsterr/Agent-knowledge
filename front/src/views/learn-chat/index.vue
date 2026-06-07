<template>
  <div class="learn-chat-page">
    <el-row :gutter="16" class="full">
      <el-col :span="8">
        <ReActChainPanel :status="status" :steps="steps" />
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="header">
              <span>LearnAgent 学习对话</span>
              <el-tag effect="plain">Trace: {{ traceId || '-' }}</el-tag>
            </div>
          </template>

          <div class="answer-box">
            <div v-if="!answer && !isRunning" class="empty">输入问题，让 Agent 先检索知识库，再展示推理链路。</div>
            <div v-if="answer" class="answer">{{ answer }}</div>
            <el-alert v-if="error" type="error" :title="error" show-icon />
          </div>

          <SourceList :sources="sources" />

          <div class="composer">
            <el-input v-model="form.message" type="textarea" :rows="4" placeholder="例如：梯度下降和反向传播是什么关系？" />
            <div class="composer-row">
              <el-input v-model="form.sessionId" placeholder="sessionId，可选" clearable />
              <el-select v-model="form.strategy" style="width: 180px">
                <el-option label="auto" value="auto" />
                <el-option label="探索" value="explore" />
                <el-option label="测验" value="quiz" />
                <el-option label="复习" value="review" />
              </el-select>
              <el-switch v-model="form.memoryEnabled" active-text="Memory" inactive-text="无记忆" />
              <el-button type="primary" :loading="isRunning" @click="handleSend">发送</el-button>
              <el-button :disabled="!isRunning" @click="stop">停止</el-button>
              <el-button @click="clear">清空</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import ReActChainPanel from '@/components/react/ReActChainPanel.vue'
import SourceList from '@/components/react/SourceList.vue'
import { useLearnReActStream } from '@/composables/useLearnReActStream'
import type { ReActStrategy } from '@/types/learn'

const { status, traceId, sessionId, answer, error, steps, sources, isRunning, start, stop, clear } = useLearnReActStream()

const form = reactive({
  message: '',
  sessionId: '',
  strategy: 'auto' as ReActStrategy,
  memoryEnabled: true,
})

const handleSend = async () => {
  const message = form.message.trim()
  if (!message) {
    ElMessage.warning('请输入学习问题')
    return
  }
  form.message = ''
  try {
    await start(message, form.sessionId || undefined, form.strategy, form.memoryEnabled)
    if (sessionId.value) form.sessionId = sessionId.value
  } catch (err: any) {
    ElMessage.error(err?.message || '发送失败')
  }
}
</script>

<style scoped lang="scss">
.learn-chat-page { height: calc(100vh - 120px); }
.full, .panel { height: 100%; }
.panel :deep(.el-card__body) { height: calc(100% - 58px); display: flex; flex-direction: column; }
.header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.empty { color: #909399; line-height: 1.8; }
.step-card { border: 1px solid #ebeef5; border-radius: 8px; padding: 10px; background: #fafafa; line-height: 1.7; }
.step-type { font-weight: 700; color: #409eff; margin-bottom: 4px; }
.answer-box { flex: 1; overflow: auto; border: 1px solid #ebeef5; border-radius: 8px; padding: 14px; background: #fff; }
.answer { white-space: pre-wrap; line-height: 1.9; color: #1f2937; }
.composer { margin-top: 12px; }
.composer-row { display: flex; gap: 8px; margin-top: 8px; }
</style>
