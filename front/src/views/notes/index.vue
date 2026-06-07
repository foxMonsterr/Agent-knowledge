<template>
  <div class="notes-page">
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>创建笔记</template>
          <el-form label-position="top">
            <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
            <el-form-item label="标签"><el-input v-model="tagText" placeholder="逗号分隔" /></el-form-item>
            <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
            <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
            <el-button type="primary" @click="handleCreate">保存笔记</el-button>
          </el-form>
        </el-card>

        <el-card shadow="never" class="mt16">
          <template #header>导入资料</template>
          <el-upload :auto-upload="false" :limit="1" accept=".md,.txt,.pdf,.docx" :on-change="handleFileChange">
            <el-button>选择 md/txt/pdf/docx 文件</el-button>
          </el-upload>
          <el-button class="mt8" type="primary" plain @click="handleImport">导入</el-button>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="header">
              <span>知识笔记</span>
              <el-button @click="load">刷新</el-button>
            </div>
          </template>
          <el-table :data="notes" height="650">
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="summary" label="摘要" min-width="260" />
            <el-table-column prop="masteryLevel" label="掌握度" width="90" />
            <el-table-column label="标签" min-width="160">
              <template #default="{ row }">
                <el-tag v-for="tag in row.tags" :key="tag" class="tag" size="small">{{ tag }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { createNote, importNoteDocument, listNotes } from '@/api/learn'
import type { KnowledgeNote } from '@/types/learn'

const notes = ref<KnowledgeNote[]>([])
const tagText = ref('')
const selectedFile = ref<File | null>(null)
const form = reactive({ title: '', content: '', category: '' })

const load = async () => {
  notes.value = await listNotes()
}

const handleCreate = async () => {
  if (!form.title || !form.content) return ElMessage.warning('请填写标题和内容')
  await createNote({ ...form, tags: tagText.value.split(',').map(t => t.trim()).filter(Boolean) })
  ElMessage.success('已保存')
  form.title = ''
  form.content = ''
  tagText.value = ''
  await load()
}

const handleImport = async () => {
  if (!selectedFile.value) return ElMessage.warning('请选择文件')
  await importNoteDocument(selectedFile.value)
  ElMessage.success('导入完成')
  await load()
}

const handleFileChange = (file: UploadFile) => {
  selectedFile.value = file.raw || null
}

onMounted(load)
</script>

<style scoped>
.mt16 { margin-top: 16px; }
.mt8 { margin-top: 8px; }
.header { display: flex; justify-content: space-between; align-items: center; }
.tag { margin-right: 4px; }
</style>
