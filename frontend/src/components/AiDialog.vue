<template>
  <el-tooltip :content="t('ai.assistant')" placement="bottom">
    <el-button :icon="MagicStick" circle :aria-label="t('ai.assistant')" @click="visible = true" />
  </el-tooltip>

  <el-dialog
    v-model="visible"
    append-to-body
    align-center
    class="ai-assistant-dialog"
    width="min(720px, calc(100vw - 32px))"
    destroy-on-close
  >
    <template #header>
      <div class="ai-dialog-header">
        <span class="ai-dialog-icon">
          <el-icon><MagicStick /></el-icon>
        </span>
        <div>
          <h2>{{ t('ai.quickAssistant') }}</h2>
          <p>{{ t('ai.quickSubtitle') }}</p>
        </div>
      </div>
    </template>

    <div class="ai-dialog-body">
      <el-tabs v-model="active" class="ai-tabs">
        <el-tab-pane :label="t('ai.qa')" name="qa">
          <div class="ai-field">
            <span class="ai-field-label">{{ t('ai.questionContent') }}</span>
            <el-input
              v-model="question"
              type="textarea"
              :rows="5"
              resize="none"
              maxlength="500"
              show-word-limit
              :placeholder="t('ai.questionContent')"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane :label="t('ai.schedule')" name="schedule">
          <div class="ai-field">
            <span class="ai-field-label">{{ t('ai.scheduleText') }}</span>
            <el-input
              v-model="scheduleText"
              type="textarea"
              :rows="5"
              resize="none"
              maxlength="500"
              show-word-limit
              :placeholder="t('ai.scheduleText')"
            />
          </div>
        </el-tab-pane>
      </el-tabs>

      <section class="ai-result" :class="{ empty: !result }">
        <div class="ai-result-head">
          <strong>{{ t('ai.result') }}</strong>
          <span>{{ result ? t('ai.generated') : t('ai.waiting') }}</span>
        </div>
        <pre v-if="result">{{ result }}</pre>
        <p v-else>{{ t('ai.resultPlaceholder') }}</p>
      </section>
    </div>

    <template #footer>
      <div class="ai-dialog-footer">
        <el-button @click="visible = false">{{ t('common.close') }}</el-button>
        <el-button :icon="DocumentCopy" :disabled="!result" @click="copyResult">{{ t('ai.copyResult') }}</el-button>
        <el-button type="primary" :icon="Promotion" :loading="loading" @click="submit">{{ t('common.send') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { DocumentCopy, MagicStick, Promotion } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { aiApi } from '@/api/ai';

const visible = ref(false);
const active = ref('qa');
const question = ref('');
const scheduleText = ref('');
const result = ref('');
const loading = ref(false);
const { t } = useI18n();

async function submit() {
  const value = active.value === 'qa' ? question.value : scheduleText.value;
  if (!value.trim()) {
    ElMessage.warning(t('ai.inputFirst'));
    return;
  }

  loading.value = true;
  try {
    const response = active.value === 'qa' ? await aiApi.qa({ question: value }) : await aiApi.scheduleParse({ text: value });
    result.value = response.answer || response.content || JSON.stringify(response, null, 2);
  } finally {
    loading.value = false;
  }
}

async function copyResult() {
  await navigator.clipboard.writeText(result.value);
  ElMessage.success(t('ai.copied'));
}
</script>

<style scoped>
:deep(.ai-assistant-dialog) {
  max-height: calc(100vh - 48px);
  margin: 24px auto;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.ai-assistant-dialog .el-dialog__header) {
  padding: 20px 24px 14px;
  margin: 0;
  border-bottom: 1px solid var(--oa-border);
}

:deep(.ai-assistant-dialog .el-dialog__body) {
  padding: 18px 24px 0;
}

:deep(.ai-assistant-dialog .el-dialog__footer) {
  padding: 16px 24px 20px;
}

.ai-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-right: 30px;
}

.ai-dialog-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #eff6ff;
  color: var(--oa-primary);
  font-size: 20px;
}

.ai-dialog-header h2 {
  margin: 0;
  color: var(--oa-text);
  font-size: 18px;
  line-height: 1.35;
}

.ai-dialog-header p {
  margin: 3px 0 0;
  color: var(--oa-muted);
  font-size: 13px;
  line-height: 1.5;
}

.ai-dialog-body {
  display: grid;
  gap: 16px;
}

.ai-tabs :deep(.el-tabs__header) {
  margin: 0 0 14px;
}

.ai-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--oa-border);
}

.ai-field {
  display: grid;
  gap: 8px;
}

.ai-field-label {
  color: var(--oa-text);
  font-size: 13px;
  font-weight: 600;
}

.ai-field :deep(.el-textarea__inner) {
  min-height: 132px !important;
  border-radius: 8px;
  line-height: 1.7;
}

.ai-result {
  display: grid;
  gap: 10px;
  min-height: 132px;
  max-height: 260px;
  padding: 14px;
  border: 1px solid var(--oa-border);
  border-radius: 8px;
  background: #f8fafc;
  overflow: auto;
}

.ai-result.empty {
  align-content: start;
  background: #ffffff;
}

.ai-result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--oa-text);
}

.ai-result-head span {
  color: var(--oa-muted);
  font-size: 12px;
}

.ai-result pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: var(--oa-text);
  font-family: inherit;
}

.ai-result p {
  margin: 0;
  color: var(--oa-muted);
}

.ai-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 640px) {
  :deep(.ai-assistant-dialog .el-dialog__header),
  :deep(.ai-assistant-dialog .el-dialog__body),
  :deep(.ai-assistant-dialog .el-dialog__footer) {
    padding-right: 16px;
    padding-left: 16px;
  }

  .ai-dialog-footer {
    flex-wrap: wrap;
  }
}
</style>
