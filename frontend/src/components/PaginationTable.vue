<template>
  <section class="pagination-table panel">
    <div v-if="$slots.toolbar || title" class="pagination-table__toolbar">
      <h3 v-if="title">{{ title }}</h3>
      <slot name="toolbar" />
    </div>

    <el-table
      v-loading="loading"
      :data="data"
      :row-key="rowKey"
      :height="height"
      stripe
      @selection-change="(rows: unknown[]) => emit('selection-change', rows)"
    >
      <el-table-column v-if="selectable" type="selection" width="44" />
      <slot />
      <template #empty>
        <el-empty class="content-empty" :description="t('common.noData')" />
      </template>
    </el-table>

    <div class="pagination-table__footer">
      <el-button :icon="Refresh" text :loading="loading" @click="emit('refresh')">{{ t('common.refresh') }}</el-button>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        @update:current-page="(value: number) => emit('update:page', value)"
        @update:page-size="(value: number) => emit('update:pageSize', value)"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

withDefaults(
  defineProps<{
    data: unknown[];
    total: number;
    page: number;
    pageSize: number;
    loading?: boolean;
    rowKey?: string;
    title?: string;
    selectable?: boolean;
    height?: string | number;
  }>(),
  {
    loading: false,
    rowKey: 'id',
    title: '',
    selectable: false,
    height: undefined
  }
);

const emit = defineEmits<{
  (event: 'update:page', value: number): void;
  (event: 'update:pageSize', value: number): void;
  (event: 'refresh'): void;
  (event: 'selection-change', value: unknown[]): void;
}>();

const { t } = useI18n();
</script>

<style scoped>
.pagination-table {
  overflow: hidden;
}

.pagination-table__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 0 16px;
  border-bottom: 1px solid var(--oa-border);
}

.pagination-table__toolbar h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 650;
}

.pagination-table__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--oa-border);
}

.pagination-table__footer :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .pagination-table__footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-table__footer :deep(.el-pagination) {
    justify-content: flex-start;
  }
}
</style>
