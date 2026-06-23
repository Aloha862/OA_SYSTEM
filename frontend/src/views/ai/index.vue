<template>
  <div class="ai-workspace">
    <ConversationSidebar
      :conversations="conversations"
      :active-id="activeId"
      :open="sidebarOpen"
      @create="createConversation"
      @select="selectConversation"
      @remove="removeConversation"
      @close="sidebarOpen = false"
    />

    <main class="chat-workspace">
      <header class="chat-header">
        <div class="header-left">
          <button class="mobile-menu" type="button" aria-label="打开会话列表" @click="sidebarOpen = true"><el-icon><Menu /></el-icon></button>
          <div><strong>{{ currentConversation?.title || 'OA 智能助手' }}</strong><span><i />{{ loading ? '正在思考' : '服务就绪' }}</span></div>
        </div>
        <div class="mode-switch" aria-label="助手模式">
          <button v-for="mode in modes" :key="mode.key" type="button" :class="{ active: selectedMode === mode.key }" @click="selectedMode = mode.key">{{ mode.label }}</button>
        </div>
        <div class="header-actions">
          <button type="button" :disabled="!activeId || messages.length === 0" @click="clearCurrent"><el-icon><Delete /></el-icon><span>清空</span></button>
          <RouterLink to="/dashboard"><el-icon><Close /></el-icon></RouterLink>
        </div>
      </header>

      <section ref="scrollRef" class="message-scroll" @scroll="trackScroll">
        <div v-if="initializing" class="center-state"><span class="loader" />正在载入会话</div>
        <div v-else-if="messages.length === 0" class="empty-state">
          <div class="empty-mark">AI</div>
          <p class="eyebrow">OA INTELLIGENCE</p>
          <h1>今天想一起处理什么？</h1>
          <p>我可以协助你理解审批流程、整理通知、准备会议内容，也能回答平台使用问题。</p>
          <div class="starter-grid">
            <button v-for="starter in starters" :key="starter.title" type="button" @click="send(starter.prompt)">
              <el-icon><component :is="starter.icon" /></el-icon>
              <span><strong>{{ starter.title }}</strong><small>{{ starter.copy }}</small></span>
            </button>
          </div>
        </div>
        <template v-else>
          <ChatMessage v-for="message in messages" :key="message.id" :message="message" :user-initial="userInitial" @retry="retryLast" @regenerate="regenerate" />
          <div ref="bottomRef" />
        </template>
      </section>

      <footer class="composer-footer">
        <button v-if="showJump" class="jump-bottom" type="button" @click="scrollToBottom(true)"><el-icon><ArrowDown /></el-icon>回到底部</button>
        <ChatComposer :loading="loading" @send="send" @stop="stop" />
      </footer>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Calendar, ChatDotRound, Close, Delete, Document, EditPen, Menu } from '@element-plus/icons-vue';
import { aiApi, regenerateAiMessage, streamAiMessage } from '@/api/ai';
import type { AiConversation, AiMessage } from '@/api/types';
import { useUserStore } from '@/stores/user';
import ChatComposer from './components/ChatComposer.vue';
import ChatMessage from './components/ChatMessage.vue';
import ConversationSidebar from './components/ConversationSidebar.vue';

const conversations = ref<AiConversation[]>([]);
const activeId = ref<number>();
const messages = ref<AiMessage[]>([]);
const loading = ref(false);
const initializing = ref(true);
const sidebarOpen = ref(false);
const selectedMode = ref('QA');
const scrollRef = ref<HTMLElement>();
const bottomRef = ref<HTMLElement>();
const showJump = ref(false);
let controller: AbortController | undefined;

const userStore = useUserStore();
const userInitial = computed(() => (userStore.userInfo?.realName || userStore.userInfo?.username || '你').slice(0, 1));
const currentConversation = computed(() => conversations.value.find(item => item.id === activeId.value));
const modes = [
  { key: 'QA', label: '问答' },
  { key: 'NEWS_GENERATE', label: '新闻创作' },
  { key: 'NEWS_POLISH', label: '内容润色' },
  { key: 'SCHEDULE_PARSE', label: '日程助手' }
];
const starters = [
  { title: '审批流程', copy: '了解请假、报销与审批操作', prompt: '请说明从发起请假到审批完成的完整流程。', icon: Document },
  { title: '会议通知', copy: '整理清晰、专业的会议邀请', prompt: '帮我写一份明天下午项目复盘会的会议通知。', icon: Calendar },
  { title: '新闻草稿', copy: '生成适合企业内部发布的内容', prompt: '帮我生成一篇关于团队季度表彰的内部新闻草稿。', icon: EditPen },
  { title: '平台帮助', copy: '快速找到 OA 功能与操作入口', prompt: '介绍一下这个 OA 系统能完成哪些核心工作。', icon: ChatDotRound }
];

onMounted(loadConversations);

async function loadConversations() {
  initializing.value = true;
  try {
    conversations.value = await aiApi.conversations();
    if (conversations.value.length) await selectConversation(conversations.value[0].id);
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    if (message.includes('doesn\'t exist') || message.includes('不存在')) ElMessage.warning('AI 会话表尚未初始化，请先执行数据库升级脚本。');
  } finally {
    initializing.value = false;
  }
}

async function createConversation() {
  if (loading.value) stop();
  const item = await aiApi.createConversation({ mode: selectedMode.value });
  conversations.value.unshift(item);
  activeId.value = item.id;
  messages.value = [];
  sidebarOpen.value = false;
}

async function ensureConversation() {
  if (!activeId.value) await createConversation();
  return activeId.value!;
}

async function selectConversation(id: number) {
  if (loading.value) stop();
  activeId.value = id;
  messages.value = await aiApi.messages(id);
  selectedMode.value = conversations.value.find(item => item.id === id)?.mode || 'QA';
  sidebarOpen.value = false;
  await nextTick();
  scrollToBottom(true);
}

async function removeConversation(item: AiConversation) {
  await ElMessageBox.confirm(`删除会话“${item.title}”？此操作不可恢复。`, '删除会话', { type: 'warning' });
  await aiApi.removeConversation(item.id);
  conversations.value = conversations.value.filter(row => row.id !== item.id);
  if (activeId.value === item.id) {
    activeId.value = undefined;
    messages.value = [];
    if (conversations.value[0]) await selectConversation(conversations.value[0].id);
  }
}

async function clearCurrent() {
  if (!activeId.value) return;
  await ElMessageBox.confirm('清空当前会话中的全部消息？', '清空会话', { type: 'warning' });
  await aiApi.clearConversation(activeId.value);
  messages.value = [];
  const item = currentConversation.value;
  if (item) item.title = '新对话';
}

async function send(content: string) {
  if (loading.value || !content.trim()) return;
  const conversationId = await ensureConversation();
  const clientMessageId = crypto.randomUUID();
  const timestamp = Date.now();
  const userMessage: AiMessage = { id: -timestamp, conversationId, role: 'USER', content: content.trim(), status: 'COMPLETED' };
  const assistant: AiMessage = { id: -(timestamp + 1), conversationId, role: 'ASSISTANT', content: '', status: 'STREAMING' };
  messages.value.push(userMessage, assistant);
  loading.value = true;
  controller = new AbortController();
  await nextTick();
  scrollToBottom(true);
  try {
    await streamAiMessage(conversationId, content.trim(), clientMessageId, {
      onMeta(data) {
        userMessage.id = Number(data.userMessageId || userMessage.id);
        assistant.id = Number(data.assistantMessageId || assistant.id);
      },
      onDelta(delta) {
        assistant.content += delta;
        if (!showJump.value) nextTick(() => scrollToBottom());
      },
      onDone(data) {
        assistant.status = 'COMPLETED';
        assistant.costTimeMs = Number(data.costTimeMs || 0);
      },
      onError(data) {
        assistant.status = 'FAILED';
        assistant.errorMessage = data.message || 'AI 服务暂时不可用。';
      }
    }, controller.signal);
    await refreshConversationList();
  } catch (error) {
    assistant.status = 'FAILED';
    assistant.errorMessage = controller.signal.aborted ? '已停止生成。' : error instanceof Error ? error.message : '请求失败，请重试。';
  } finally {
    loading.value = false;
    controller = undefined;
  }
}

function stop() { controller?.abort(); loading.value = false; }
function retryLast() { const last = [...messages.value].reverse().find(item => item.role === 'USER'); if (last?.content) send(last.content); }
async function regenerate(previous: AiMessage) {
  if (loading.value || !activeId.value || previous.id <= 0) return;
  const assistant: AiMessage = { id: -Date.now(), conversationId: activeId.value, role: 'ASSISTANT', content: '', status: 'STREAMING' };
  const previousIndex = messages.value.findIndex(item => item.id === previous.id);
  if (previousIndex < 0) return;
  messages.value.splice(previousIndex, 1, assistant);
  loading.value = true;
  controller = new AbortController();
  await nextTick();
  scrollToBottom(true);
  try {
    await regenerateAiMessage(activeId.value, previous.id, {
      onMeta(data) { assistant.id = Number(data.assistantMessageId || assistant.id); },
      onDelta(delta) { assistant.content += delta; if (!showJump.value) nextTick(() => scrollToBottom()); },
      onDone(data) { assistant.status = 'COMPLETED'; assistant.costTimeMs = Number(data.costTimeMs || 0); },
      onError(data) {
        messages.value.splice(previousIndex, 1, previous);
        ElMessage.error(data.message || '重新生成失败，已保留原回答。');
      }
    }, controller.signal);
  } catch (error) {
    messages.value.splice(previousIndex, 1, previous);
    if (!controller.signal.aborted) {
      ElMessage.error(error instanceof Error ? error.message : '重新生成失败，已保留原回答。');
    }
  } finally {
    loading.value = false;
    controller = undefined;
  }
}
async function refreshConversationList() { conversations.value = await aiApi.conversations(); }
function trackScroll() { const node = scrollRef.value; if (!node) return; showJump.value = node.scrollHeight - node.scrollTop - node.clientHeight > 140; }
function scrollToBottom(force = false) { if (force) showJump.value = false; bottomRef.value?.scrollIntoView({ behavior: force ? 'auto' : 'smooth', block: 'end' }); }
</script>

<style scoped>
.ai-workspace { display: flex; height: 100svh; overflow: hidden; background: #fbfcfe; color: #172033; }
.chat-workspace { display: grid; grid-template-rows: 64px minmax(0, 1fr) auto; flex: 1; min-width: 0; height: 100svh; }
.chat-header { z-index: 5; display: grid; grid-template-columns: minmax(180px, 1fr) auto minmax(150px, 1fr); gap: 18px; align-items: center; padding: 0 22px; border-bottom: 1px solid #e7ebf1; background: rgba(255,255,255,.9); backdrop-filter: blur(16px); }
.header-left { display: flex; gap: 10px; align-items: center; min-width: 0; }
.header-left strong, .header-left span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.header-left strong { font-size: 14px; }
.header-left span { margin-top: 3px; color: #8792a2; font-size: 10px; }
.header-left span i { display: inline-block; width: 6px; height: 6px; margin-right: 5px; border-radius: 50%; background: #20a46b; }
.mobile-menu { display: none; }
.mode-switch { display: flex; gap: 3px; padding: 3px; border-radius: 10px; background: #f0f3f7; }
.mode-switch button { padding: 7px 10px; border: 0; border-radius: 8px; background: transparent; color: #6d7889; cursor: pointer; font-size: 12px; }
.mode-switch button.active { background: white; color: #245eea; box-shadow: 0 2px 8px rgba(29,46,75,.08); }
.header-actions { display: flex; gap: 7px; align-items: center; justify-content: flex-end; }
.header-actions button, .header-actions a, .mobile-menu { display: inline-flex; gap: 6px; align-items: center; justify-content: center; height: 36px; padding: 0 10px; border: 1px solid #e0e6ef; border-radius: 10px; background: white; color: #687487; cursor: pointer; text-decoration: none; }
.header-actions button:disabled { opacity: .45; cursor: default; }
.message-scroll { min-height: 0; overflow-y: auto; overscroll-behavior: contain; }
.center-state { display: flex; gap: 10px; align-items: center; justify-content: center; height: 100%; color: #778396; }
.loader { width: 20px; height: 20px; border: 2px solid #dbe4f4; border-top-color: #245eea; border-radius: 50%; animation: spin .8s linear infinite; }
.empty-state { display: grid; place-items: center; align-content: center; width: min(820px, calc(100% - 34px)); min-height: 100%; margin: 0 auto; padding: 42px 0 120px; text-align: center; }
.empty-mark { display: grid; width: 52px; height: 52px; place-items: center; border-radius: 16px; background: #245eea; color: white; font-weight: 900; box-shadow: 0 14px 30px rgba(36,94,234,.24); }
.eyebrow { margin: 16px 0 5px !important; color: #245eea !important; font-size: 10px !important; font-weight: 800; letter-spacing: .1em; }
.empty-state h1 { margin: 0; font-size: clamp(28px, 4vw, 42px); letter-spacing: -.04em; }
.empty-state > p { max-width: 600px; margin: 13px 0 0; color: #748095; line-height: 1.7; }
.starter-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; width: min(650px, 100%); margin-top: 30px; text-align: left; }
.starter-grid button { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 10px; align-items: center; padding: 14px; border: 1px solid #e0e6ef; border-radius: 13px; background: transparent; color: #263247; cursor: pointer; }
.starter-grid button:hover { border-color: #9cb8f8; background: white; transform: translateY(-1px); }
.starter-grid .el-icon { color: #245eea; font-size: 20px; }
.starter-grid strong, .starter-grid small { display: block; }
.starter-grid small { margin-top: 3px; color: #8994a4; }
.composer-footer { position: relative; padding: 12px 0 16px; background: linear-gradient(180deg, rgba(251,252,254,0), #fbfcfe 28%); }
.jump-bottom { position: absolute; left: 50%; top: -32px; display: flex; gap: 5px; align-items: center; transform: translateX(-50%); padding: 7px 10px; border: 1px solid #dbe3ee; border-radius: 999px; background: white; color: #637084; cursor: pointer; box-shadow: 0 8px 20px rgba(29,45,70,.1); }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 1060px) { .chat-header { grid-template-columns: minmax(160px, 1fr) auto; } .mode-switch { display: none; } }
@media (max-width: 860px) { .mobile-menu { display: inline-flex; flex: none; padding: 0; width: 36px; } .chat-header { padding: 0 12px; } .header-actions button span { display: none; } }
@media (max-width: 640px) { .chat-workspace { grid-template-rows: 58px minmax(0, 1fr) auto; } .starter-grid { grid-template-columns: 1fr; } .empty-state { align-content: start; padding-top: 68px; } .empty-state h1 { font-size: 30px; } .composer-footer { padding-bottom: max(10px, env(safe-area-inset-bottom)); } }
</style>
