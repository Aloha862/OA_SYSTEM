<template>
  <el-breadcrumb class="oa-breadcrumb" separator="/">
    <el-breadcrumb-item v-for="item in crumbs" :key="item.path" :to="item.path">
      {{ item.meta.titleKey ? t(item.meta.titleKey) : item.meta.title }}
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';

const route = useRoute();
const { t } = useI18n();
const crumbs = computed(() => route.matched.filter((item) => (item.meta.titleKey || item.meta.title) && item.path !== '/'));
</script>

<style scoped>
.oa-breadcrumb {
  min-width: 0;
  color: var(--oa-muted);
}

.oa-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--oa-text);
  font-weight: 600;
}
</style>
