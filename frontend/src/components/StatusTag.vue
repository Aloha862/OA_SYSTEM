<template>
  <el-tag :type="tag.type" :effect="effect" :class="['status-tag', `status-tag--${String(status).toLowerCase()}`]">
    {{ tag.label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const props = withDefaults(
  defineProps<{
    status?: string | number | boolean;
    mode?: 'approval' | 'news' | 'schedule' | 'user' | 'read' | 'risk' | 'generic';
    effect?: 'dark' | 'light' | 'plain';
  }>(),
  {
    status: '',
    mode: 'generic',
    effect: 'light'
  }
);

const { t } = useI18n();

const maps: Record<string, Record<string, { labelKey: string; type: '' | 'success' | 'info' | 'warning' | 'danger' }>> = {
  approval: {
    DRAFT: { labelKey: 'status.draft', type: 'info' },
    PENDING: { labelKey: 'status.pending', type: 'warning' },
    APPROVED: { labelKey: 'status.approved', type: 'success' },
    REJECTED: { labelKey: 'status.rejected', type: 'danger' },
    WITHDRAWN: { labelKey: 'status.withdrawn', type: 'info' }
  },
  news: {
    DRAFT: { labelKey: 'status.draft', type: 'info' },
    PUBLISHED: { labelKey: 'status.published', type: 'success' },
    OFFLINE: { labelKey: 'status.offline', type: 'warning' }
  },
  schedule: {
    NORMAL: { labelKey: 'status.normal', type: 'success' },
    CANCELLED: { labelKey: 'status.cancelled', type: 'info' },
    FINISHED: { labelKey: 'status.finished', type: 'success' }
  },
  user: {
    '1': { labelKey: 'status.enabled', type: 'success' },
    '0': { labelKey: 'status.disabled', type: 'danger' },
    true: { labelKey: 'status.enabled', type: 'success' },
    false: { labelKey: 'status.disabled', type: 'danger' }
  },
  read: {
    '1': { labelKey: 'status.read', type: 'info' },
    '0': { labelKey: 'status.unread', type: 'warning' }
  },
  risk: {
    LOW: { labelKey: 'status.lowRisk', type: 'success' },
    MEDIUM: { labelKey: 'status.mediumRisk', type: 'warning' },
    HIGH: { labelKey: 'status.highRisk', type: 'danger' }
  }
};

const tag = computed(() => {
  const key = String(props.status);
  const found = maps[props.mode]?.[key];
  return found ? { label: t(found.labelKey), type: found.type } : { label: key || '-', type: '' as const };
});
</script>
