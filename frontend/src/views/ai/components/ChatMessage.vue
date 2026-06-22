<template>
  <article class="message" :class="message.role.toLowerCase()">
    <div class="avatar">{{ message.role === 'USER' ? userInitial : 'AI' }}</div>
    <div class="message-main">
      <div class="message-meta">
        <strong>{{ message.role === 'USER' ? '你' : 'OA 智能助手' }}</strong>
        <span v-if="message.status === 'STREAMING'" class="streaming">正在生成</span>
        <span v-if="message.status === 'FAILED'" class="failed">生成失败</span>
      </div>
      <div v-if="message.role === 'USER'" class="user-copy">{{ message.content }}</div>
      <MarkdownRenderer v-else :content="message.content || (message.status === 'STREAMING' ? '▋' : '')" />
      <div v-if="message.status === 'FAILED'" class="error-copy">{{ message.errorMessage || '请求未完成，请重试。' }}</div>
      <div v-if="message.role === 'ASSISTANT' && message.content" class="message-actions">
        <button type="button" @click="copy"><el-icon><DocumentCopy /></el-icon>{{ copied ? '已复制' : '复制' }}</button>
        <button v-if="message.id > 0 && message.status !== 'STREAMING'" type="button" @click="$emit('regenerate', message)"><el-icon><RefreshRight /></el-icon>重新生成</button>
        <button v-else-if="message.status === 'FAILED'" type="button" @click="$emit('retry')"><el-icon><RefreshRight /></el-icon>重试</button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { DocumentCopy, RefreshRight } from '@element-plus/icons-vue';
import type { AiMessage } from '@/api/types';
import MarkdownRenderer from './MarkdownRenderer.vue';

const props = defineProps<{ message: AiMessage; userInitial: string }>();
defineEmits<{ retry: []; regenerate: [message: AiMessage] }>();
const copied = ref(false);
async function copy() {
  await navigator.clipboard.writeText(props.message.content || '');
  copied.value = true;
  window.setTimeout(() => (copied.value = false), 1600);
}
</script>

<style scoped>
.message { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 13px; max-width: 850px; margin: 0 auto; padding: 22px 24px; }
.message.user { direction: rtl; }
.message.user > * { direction: ltr; }
.avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; background: #235fe5; color: white; font-size: 12px; font-weight: 800; }
.user .avatar { background: #172033; }
.message-main { min-width: 0; }
.message-meta { display: flex; gap: 9px; align-items: center; margin-bottom: 8px; color: #6a7688; font-size: 12px; }
.message-meta strong { color: #273244; font-size: 13px; }
.user .message-meta { justify-content: flex-end; }
.user-copy { display: inline-block; float: right; max-width: min(680px, 92%); padding: 12px 15px; border-radius: 16px 5px 16px 16px; background: #e9f0ff; color: #18233a; line-height: 1.65; white-space: pre-wrap; }
.streaming { color: #245eea; }
.failed, .error-copy { color: #c43c45; }
.error-copy { margin-top: 8px; font-size: 13px; }
.message-actions { display: flex; gap: 4px; margin-top: 10px; }
.message-actions button { display: inline-flex; gap: 5px; align-items: center; padding: 5px 8px; border: 0; border-radius: 7px; background: transparent; color: #6b7788; cursor: pointer; }
.message-actions button:hover { background: #eef2f7; color: #245eea; }
@media (max-width: 640px) { .message { padding: 18px 16px; grid-template-columns: 30px minmax(0, 1fr); gap: 10px; } .avatar { width: 30px; height: 30px; } }
</style>
