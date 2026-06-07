<template>
  <div v-if="sources.length" class="source-list">
    <div class="section-title">引用来源</div>
    <el-tag
      v-for="source in sources"
      :key="source.sourceId"
      class="source-tag"
      type="success"
      effect="plain"
    >
      {{ source.title }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import type { ReActSourceRef } from '@/types/react'

defineProps<{
  sources: ReActSourceRef[]
}>()
</script>

<style scoped lang="scss">
.source-list { margin-top: 12px; }
.section-title { font-weight: 700; margin-bottom: 8px; }
.source-tag { margin: 0 8px 8px 0; }
</style>
