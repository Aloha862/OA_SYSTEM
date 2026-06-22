<template>
  <el-config-provider :locale="elementLocale">
    <RouterView />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watchEffect } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';
import { useLocaleStore } from '@/stores/locale';

const route = useRoute();
const { t } = useI18n();
const localeStore = useLocaleStore();

const elementLocale = computed(() => (localeStore.locale === 'en-US' ? en : zhCn));

watchEffect(() => {
  const key = route.meta.titleKey || route.meta.title;
  document.title = `${key ? t(String(key)) : t('app.workspace')} - ${t('app.name')}`;
});
</script>
