<template>
  <section :class="['search-panel', { panel: !plain, 'is-plain': plain }]">
    <el-form :model="model" :inline="true" label-width="auto" class="search-panel__form" @submit.prevent>
      <slot />
      <div class="search-panel__actions">
        <el-button :icon="Search" type="primary" :loading="loading" @click="emit('search')">{{ t('common.search') }}</el-button>
        <el-button :icon="RefreshLeft" @click="emit('reset')">{{ t('common.reset') }}</el-button>
        <slot name="actions" />
      </div>
    </el-form>
  </section>
</template>

<script setup lang="ts">
import { RefreshLeft, Search } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

defineProps<{
  model: Record<string, unknown>;
  loading?: boolean;
  plain?: boolean;
}>();

const emit = defineEmits<{
  (event: 'search'): void;
  (event: 'reset'): void;
}>();

const { t } = useI18n();
</script>

<style scoped>
.search-panel {
  padding: 14px 16px 2px;
}

.search-panel.is-plain {
  padding: 0;
}

.search-panel__form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 0 12px;
}

.search-panel__form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 12px;
}

.search-panel__form :deep(.el-input),
.search-panel__form :deep(.el-select),
.search-panel__form :deep(.el-date-editor) {
  width: 190px;
}

.search-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .search-panel__form :deep(.el-input),
  .search-panel__form :deep(.el-select),
  .search-panel__form :deep(.el-date-editor) {
    width: 100%;
  }

  .search-panel__form :deep(.el-form-item),
  .search-panel__actions {
    width: 100%;
  }
}
</style>
