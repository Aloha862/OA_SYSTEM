<template>
  <div class="composer-shell">
    <div class="quick-row">
      <button v-for="item in suggestions" :key="item" type="button" :disabled="loading" @click="text = item">{{ item }}</button>
    </div>
    <div class="composer" :class="{ focused, loading }">
      <textarea
        ref="textareaRef"
        v-model="text"
        rows="1"
        maxlength="8000"
        placeholder="向 OA 智能助手提问…"
        :disabled="loading"
        @focus="focused = true"
        @blur="focused = false"
        @input="resize"
        @keydown="onKeydown"
      />
      <button v-if="loading" class="send stop" type="button" aria-label="停止生成" @click="$emit('stop')"><span /></button>
      <button v-else class="send" type="button" :disabled="!text.trim()" aria-label="发送" @click="submit"><el-icon><Promotion /></el-icon></button>
    </div>
    <p>Enter 发送，Shift + Enter 换行。AI 生成内容仅供参考。</p>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';
import { Promotion } from '@element-plus/icons-vue';

defineProps<{ loading: boolean }>();
const emit = defineEmits<{ send: [content: string]; stop: [] }>();
const text = ref('');
const focused = ref(false);
const textareaRef = ref<HTMLTextAreaElement>();
const suggestions = ['如何发起请假审批？', '帮我整理一份会议通知', '解释当前报销流程'];
function submit() { const value = text.value.trim(); if (!value) return; emit('send', value); text.value = ''; nextTick(resize); }
function onKeydown(event: KeyboardEvent) { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submit(); } }
function resize() { const node = textareaRef.value; if (!node) return; node.style.height = 'auto'; node.style.height = `${Math.min(node.scrollHeight, 180)}px`; }
</script>

<style scoped>
.composer-shell { width: min(850px, calc(100% - 36px)); margin: 0 auto; }
.quick-row { display: flex; gap: 8px; margin-bottom: 10px; overflow-x: auto; scrollbar-width: none; }
.quick-row button { flex: none; padding: 7px 10px; border: 1px solid #dbe3ef; border-radius: 999px; background: rgba(255,255,255,.88); color: #5e6a7d; cursor: pointer; }
.quick-row button:hover { border-color: #9db8f8; color: #245eea; }
.composer { display: grid; grid-template-columns: minmax(0, 1fr) 42px; gap: 10px; align-items: end; padding: 12px 12px 12px 17px; border: 1px solid #d4deec; border-radius: 18px; background: white; box-shadow: 0 14px 40px rgba(29, 49, 82, .1); transition: border-color .18s, box-shadow .18s; }
.composer.focused { border-color: #86a9fa; box-shadow: 0 16px 44px rgba(36, 94, 234, .13); }
textarea { width: 100%; max-height: 180px; padding: 7px 0; border: 0; outline: 0; resize: none; background: transparent; color: #172033; font: inherit; line-height: 1.55; }
.send { display: grid; width: 42px; height: 42px; place-items: center; border: 0; border-radius: 13px; background: #245eea; color: white; cursor: pointer; font-size: 18px; }
.send:disabled { background: #dbe3ef; color: #98a3b4; cursor: default; }
.send.stop span { width: 12px; height: 12px; border-radius: 3px; background: white; }
p { margin: 8px 0 0; color: #8994a4; font-size: 11px; text-align: center; }
</style>
