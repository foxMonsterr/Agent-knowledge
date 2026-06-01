<template>
  <div class="page-wrap">
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>用户管理</span>
          <el-button type="primary" plain @click="load">刷新</el-button>
        </div>
      </template>

      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="role" label="角色" width="120" />
        <el-table-column prop="enabled" label="启用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button size="small" @click="switchRole(row)">切换角色</el-button>
            <el-button size="small" :type="row.enabled ? 'warning' : 'success'" @click="toggleEnabled(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeUserRole, getUserList, toggleUserEnabled, type UserVO } from '@/api/admin'

const users = ref<UserVO[]>([])

const load = async () => {
  users.value = await getUserList()
}

const toggleEnabled = async (row: UserVO) => {
  await toggleUserEnabled(row.id, !row.enabled)
  ElMessage.success('操作成功')
  await load()
}

const switchRole = async (row: UserVO) => {
  const nextRole = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
  await ElMessageBox.confirm(`确定将 ${row.username} 角色切换为 ${nextRole} 吗？`, '提示')
  await changeUserRole(row.id, nextRole)
  ElMessage.success('角色修改成功')
  await load()
}

onMounted(load)
</script>

<style scoped>
.page-wrap { padding: 12px; }
.header { display: flex; justify-content: space-between; align-items: center; }
</style>
