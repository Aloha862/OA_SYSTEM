<template>
  <aside class="conversation-sidebar" :class="{ open }">
    <div class="brand"><span>AI</span><div><strong>OA 智能助手</strong><small>企业协作知识工作台</small></div><button type="button" @click="$emit('close')">×</button></div>
    <button class="new-chat" type="button" @click="$emit('create')"><el-icon><Plus /></el-icon>新建会话</button>
    <label class="search"><el-icon><Search /></el-icon><input v-model="keyword" placeholder="搜索会话" /></label>
    <div class="conversation-list">
      <p v-if="filtered.length === 0" class="empty">暂无历史会话</p>
      <button v-for="item in filtered" :key="item.id" class="conversation-item" :class="{ active: item.id === activeId }" type="button" @click="$emit('select', item.id)">
        <span><strong>{{ item.title }}</strong><small>{{ formatTime(item.lastMessageAt || item.createdAt) }}</small></span>
        <i title="删除" @click.stop="$emit('remove', item)">×</i>
      </button>
    </div>
    <RouterLink class="back" to="/dashboard"><el-icon><Back /></el-icon>返回 OA 工作台</RouterLink>
  </aside>
  <button v-if="open" class="sidebar-mask" type="button" aria-label="关闭会话列表" @click="$emit('close')" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Back, Plus, Search } from '@element-plus/icons-vue';
import type { AiConversation } from '@/api/types';

const props = defineProps<{ conversations: AiConversation[]; activeId?: number; open: boolean }>();
defineEmits<{ create: []; select: [id: number]; remove: [item: AiConversation]; close: [] }>();
const keyword = ref('');
const filtered = computed(() => props.conversations.filter(item => item.title.toLowerCase().includes(keyword.value.trim().toLowerCase())));
function formatTime(value?: string) { if (!value) return ''; const d = new Date(value); return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString([], { month: 'short', day: 'numeric' }); }
</script>

<style scoped>
.conversation-sidebar { position: relative; z-index: 20; display: flex; flex-direction: column; width: 282px; min-height: 100svh; padding: 18px 14px; border-right: 1px solid #e2e8f1; background: #f7f9fc; }
.brand { display: grid; grid-template-columns: 38px minmax(0, 1fr) 24px; gap: 10px; align-items: center; padding: 4px 6px 20px; }
.brand > span { display: grid; height: 38px; place-items: center; border-radius: 12px; background: #245eea; color: white; font-weight: 900; }
.brand strong, .brand small { display: block; }
.brand small { margin-top: 2px; color: #8a95a5; font-size: 10px; }
.brand button { display: none; border: 0; background: transparent; color: #748094; font-size: 24px; }
.new-chat { display: flex; gap: 8px; align-items: center; justify-content: center; min-height: 42px; border: 1px solid #cbd8ef; border-radius: 11px; background: white; color: #245eea; font-weight: 700; cursor: pointer; }
.search { display: flex; gap: 8px; align-items: center; margin: 14px 2px 8px; padding: 9px 11px; border-radius: 10px; background: #edf1f6; color: #8a95a5; }
.search input { min-width: 0; border: 0; outline: 0; background: transparent; color: #273244; }
.conversation-list { flex: 1; overflow: auto; }
.conversation-item { display: grid; grid-template-columns: minmax(0, 1fr) 22px; gap: 5px; width: 100%; padding: 11px 10px; border: 0; border-radius: 10px; background: transparent; color: #374255; cursor: pointer; text-align: left; }
.conversation-item:hover, .conversation-item.active { background: white; box-shadow: 0 3px 14px rgba(34, 51, 84, .06); }
.conversation-item.active { color: #245eea; }
.conversation-item strong, .conversation-item small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conversation-item small { margin-top: 4px; color: #9aa4b2; font-size: 10px; }
.conversation-item i { display: grid; place-items: center; border-radius: 6px; color: transparent; font-style: normal; }
.conversation-item:hover i { color: #8994a5; }
.empty { color: #99a3b1; text-align: center; }
.back { display: flex; gap: 8px; align-items: center; padding: 12px 10px 2px; color: #687488; text-decoration: none; }
.sidebar-mask { display: none; }
@media (max-width: 860px) {
  .conversation-sidebar { position: fixed; inset: 0 auto 0 0; width: min(320px, 86vw); transform: translateX(-105%); transition: transform .22s ease; box-shadow: 18px 0 50px rgba(20,32,53,.18); }
  .conversation-sidebar.open { transform: translateX(0); }
  .brand button { display: block; }
  .sidebar-mask { position: fixed; z-index: 15; inset: 0; display: block; border: 0; background: rgba(18,28,45,.28); }
}
</style>
