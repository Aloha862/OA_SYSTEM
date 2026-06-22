<template>
  <div class="page ai-page">
    <section class="ai-hero">
      <div>
        <span class="ai-kicker">OA Intelligence</span>
        <h1>AI 助手</h1>
        <p>把智能问答和新闻创作集中在一个工作台里。</p>
      </div>
      <div class="ai-hero__meta">
        <span>{{ currentFunction?.label }}</span>
        <strong>{{ lastResponse?.provider || '待调用' }}</strong>
      </div>
    </section>

    <section class="ai-console">
      <aside class="panel ai-menu">
        <div class="ai-menu__head">
          <strong>功能</strong>
          <span>{{ availableFunctions.length }} 项</span>
        </div>
        <button
          v-for="item in availableFunctions"
          :key="item.key"
          type="button"
          :class="{ active: active === item.key }"
          @click="active = item.key"
        >
          <span class="ai-menu__icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
        </button>
      </aside>

      <div class="ai-main">
        <section class="panel panel-body ai-compose">
          <div class="section-head">
            <div>
              <h2>{{ currentFunction?.label }}</h2>
              <p>{{ currentFunction?.description }}</p>
            </div>
            <el-tag v-if="currentFunction?.adminOnly" type="warning" effect="light">管理员</el-tag>
            <el-tag v-else type="primary" effect="light">通用</el-tag>
          </div>

          <el-form class="ai-form" label-position="top">
            <template v-if="active === 'qa'">
              <el-form-item label="问题">
                <el-input
                  v-model="qa.question"
                  type="textarea"
                  :rows="8"
                  resize="none"
                  maxlength="800"
                  show-word-limit
                  placeholder="例如：请假申请被驳回后应该怎么重新提交？"
                />
              </el-form-item>
            </template>

            <template v-if="active === 'newsGenerate'">
              <div class="form-grid">
                <el-form-item label="主题" class="full">
                  <el-input v-model.trim="newsGenerate.topic" placeholder="请输入新闻主题" />
                </el-form-item>
                <el-form-item label="关键词" class="full">
                  <el-input v-model.trim="newsGenerate.keywords" placeholder="多个关键词用逗号分隔" />
                </el-form-item>
                <el-form-item label="语气">
                  <el-select v-model="newsGenerate.tone">
                    <el-option label="正式" value="formal" />
                    <el-option label="温和" value="warm" />
                    <el-option label="简洁" value="concise" />
                  </el-select>
                </el-form-item>
                <el-form-item label="字数">
                  <el-input-number v-model="newsGenerate.wordCount" :min="200" :max="2000" :step="100" />
                </el-form-item>
              </div>
            </template>

            <template v-if="active === 'newsPolish'">
              <div class="form-grid">
                <el-form-item label="原标题" class="full">
                  <el-input v-model.trim="newsPolish.title" placeholder="请输入原标题" />
                </el-form-item>
                <el-form-item label="原正文" class="full">
                  <el-input
                    v-model="newsPolish.content"
                    type="textarea"
                    :rows="8"
                    resize="none"
                    maxlength="3000"
                    show-word-limit
                    placeholder="请输入需要润色的新闻正文"
                  />
                </el-form-item>
                <el-form-item label="风格">
                  <el-select v-model="newsPolish.style">
                    <el-option label="正式清晰" value="formal" />
                    <el-option label="简洁有力" value="concise" />
                    <el-option label="温和亲切" value="warm" />
                  </el-select>
                </el-form-item>
              </div>
            </template>
          </el-form>

          <div class="ai-actions">
            <el-button @click="clearCurrent">清空</el-button>
            <el-button type="primary" :icon="Promotion" :loading="loading" @click="submit">生成结果</el-button>
          </div>
        </section>

        <section class="panel panel-body ai-output">
          <div class="section-head">
            <div>
              <h2>输出结果</h2>
              <p>{{ resultText ? '已生成内容' : '等待模型返回' }}</p>
            </div>
            <el-button :icon="DocumentCopy" :disabled="!resultText" @click="copyResult">复制</el-button>
          </div>

          <div class="ai-output__body">
            <el-skeleton v-if="loading" :rows="8" animated />
            <pre v-else-if="resultText">{{ resultText }}</pre>
            <div v-else class="ai-empty">
              <el-icon><MagicStick /></el-icon>
              <strong>还没有结果</strong>
              <span>选择功能并填写内容后生成。</span>
            </div>
          </div>

          <div class="ai-output__meta">
            <span>模型来源：{{ lastResponse?.provider || '-' }}</span>
            <span>耗时：{{ lastResponse?.costTimeMs ? `${lastResponse.costTimeMs} ms` : '-' }}</span>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { ChatDotRound, DocumentCopy, EditPen, MagicStick, Promotion } from '@element-plus/icons-vue';
import { aiApi } from '@/api/ai';
import type { AiResponse } from '@/api/types';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const loading = ref(false);
const resultText = ref('');
const lastResponse = ref<AiResponse | null>(null);
const active = ref('qa');

const allFunctions = [
  { key: 'qa', label: '智能问答', description: 'OA 流程与办公问题', icon: ChatDotRound, adminOnly: false },
  { key: 'newsGenerate', label: '新闻生成', description: '生成内部新闻草稿', icon: EditPen, adminOnly: true },
  { key: 'newsPolish', label: '新闻润色', description: '优化标题与正文表达', icon: EditPen, adminOnly: true }
];

const availableFunctions = computed(() => allFunctions.filter((item) => userStore.isAdmin || !item.adminOnly));
const currentFunction = computed(() => allFunctions.find((item) => item.key === active.value));

const qa = reactive({ question: '' });
const newsGenerate = reactive({ topic: '', keywords: '', tone: 'formal', wordCount: 600 });
const newsPolish = reactive({ title: '', content: '', style: 'formal' });

watch(availableFunctions, (items) => {
  if (!items.some((item) => item.key === active.value)) active.value = items[0]?.key || 'qa';
});

watch(active, () => {
  resultText.value = '';
  lastResponse.value = null;
});

async function submit() {
  const payload = buildPayload();
  if (!payload) return;

  loading.value = true;
  resultText.value = '';
  lastResponse.value = null;
  try {
    const response =
      active.value === 'qa'
        ? await aiApi.qa(payload as { question: string })
        : active.value === 'newsGenerate'
          ? await aiApi.newsGenerate(payload)
          : await aiApi.newsPolish(payload);

    lastResponse.value = response;
    resultText.value = formatResponse(response);
  } finally {
    loading.value = false;
  }
}

function buildPayload() {
  if (active.value === 'qa') {
    if (!qa.question.trim()) return warnRequired('请输入问题');
    return { question: qa.question.trim() };
  }
  if (active.value === 'newsGenerate') {
    if (!newsGenerate.topic.trim()) return warnRequired('请输入新闻主题');
    return { ...newsGenerate, topic: newsGenerate.topic.trim(), keywords: newsGenerate.keywords.trim() };
  }
  if (!newsPolish.content.trim()) return warnRequired('请输入新闻正文');
  return { ...newsPolish, title: newsPolish.title.trim(), content: newsPolish.content.trim() };
}

function warnRequired(message: string) {
  ElMessage.warning(message);
  return null;
}

function formatResponse(response: AiResponse) {
  const segments: string[] = [];
  const pushSegment = (value?: unknown, prefix = '') => {
    if (!value) return;
    const text = `${prefix}${String(value)}`;
    if (!segments.includes(text)) segments.push(text);
  };

  if (response.title) {
    pushSegment(response.title, '标题：');
  }
  if (response.summary) {
    pushSegment(response.summary, '摘要：');
  }
  if (response.answer) {
    pushSegment(response.answer);
  }
  if (response.content) {
    pushSegment(response.content);
  }
  if (response.suggestion) {
    pushSegment(response.suggestion, '建议：');
  }
  if (response.data && typeof response.data === 'object') {
    const dataText = JSON.stringify(response.data, null, 2);
    if (dataText && dataText !== '{}' && !segments.includes(dataText)) {
      segments.push(dataText);
    }
  }
  return segments.join('\n\n');
}

function clearCurrent() {
  resultText.value = '';
  lastResponse.value = null;
  if (active.value === 'qa') qa.question = '';
  if (active.value === 'newsGenerate') Object.assign(newsGenerate, { topic: '', keywords: '', tone: 'formal', wordCount: 600 });
  if (active.value === 'newsPolish') Object.assign(newsPolish, { title: '', content: '', style: 'formal' });
}

async function copyResult() {
  await navigator.clipboard.writeText(resultText.value);
  ElMessage.success('结果已复制');
}
</script>

<style scoped>
.ai-page {
  gap: 16px;
}

.ai-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  min-height: 132px;
  padding: 26px 28px;
  border: 1px solid #d7e2f0;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(20, 184, 166, 0.08)),
    #ffffff;
}

.ai-kicker {
  color: var(--oa-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.ai-hero h1 {
  margin: 8px 0 0;
  color: var(--oa-text);
  font-size: 30px;
  line-height: 1.15;
}

.ai-hero p {
  max-width: 560px;
  margin: 9px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.ai-hero__meta {
  display: grid;
  justify-items: end;
  gap: 4px;
  color: var(--oa-muted);
}

.ai-hero__meta strong {
  color: var(--oa-text);
  font-size: 18px;
}

.ai-console {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.ai-menu {
  position: sticky;
  top: 74px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
}

.ai-menu__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 8px 10px;
  color: var(--oa-muted);
}

.ai-menu__head strong {
  color: var(--oa-text);
}

.ai-menu button {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  width: 100%;
  min-height: 58px;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--oa-text);
  cursor: pointer;
  text-align: left;
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease;
}

.ai-menu button:hover {
  background: #f8fafc;
  transform: translateY(-1px);
}

.ai-menu button.active {
  border-color: #bfd3ff;
  background: var(--oa-primary-soft);
  color: var(--oa-primary);
}

.ai-menu__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #ffffff;
  color: inherit;
  box-shadow: inset 0 0 0 1px var(--oa-border);
}

.ai-menu button strong,
.ai-menu button small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-menu button strong {
  font-size: 14px;
}

.ai-menu button small {
  margin-top: 3px;
  color: var(--oa-muted);
  font-size: 12px;
}

.ai-main {
  display: grid;
  gap: 16px;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
}

.section-head p {
  margin: 5px 0 0;
  color: var(--oa-muted);
}

.ai-form :deep(.el-form-item__label) {
  color: var(--oa-text);
  font-weight: 650;
}

.ai-form :deep(.el-textarea__inner) {
  min-height: 188px !important;
  line-height: 1.75;
}

.ai-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.ai-output__body {
  min-height: 260px;
  max-height: 520px;
  padding: 16px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: #f8fafc;
  overflow: auto;
}

.ai-output__body pre {
  margin: 0;
  color: var(--oa-text);
  font-family: inherit;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 220px;
  color: var(--oa-muted);
  text-align: center;
}

.ai-empty .el-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #ffffff;
  color: var(--oa-primary);
  font-size: 22px;
  box-shadow: inset 0 0 0 1px var(--oa-border);
}

.ai-empty strong {
  color: var(--oa-text);
}

.ai-output__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  color: var(--oa-muted);
  font-size: 12px;
}

@media (min-width: 1500px) {
  .ai-main {
    grid-template-columns: minmax(0, 1fr) minmax(380px, 0.82fr);
    align-items: start;
  }
}

@media (max-width: 980px) {
  .ai-hero,
  .ai-console {
    grid-template-columns: 1fr;
  }

  .ai-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-hero__meta {
    justify-items: start;
  }

  .ai-menu {
    position: static;
  }
}
</style>
