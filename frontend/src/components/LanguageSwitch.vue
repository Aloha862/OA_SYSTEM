<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <el-button class="language-switch" :text="text" :circle="circle" :aria-label="t('common.language')">
      <el-icon><Switch /></el-icon>
      <span v-if="!circle">{{ localeStore.locale === 'zh-CN' ? '中文' : 'EN' }}</span>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :disabled="localeStore.locale === 'zh-CN'">
          {{ t('common.chinese') }}
        </el-dropdown-item>
        <el-dropdown-item command="en-US" :disabled="localeStore.locale === 'en-US'">
          {{ t('common.english') }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { Switch } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { useLocaleStore } from '@/stores/locale';
import type { SupportedLocale } from '@/i18n';

withDefaults(
  defineProps<{
    circle?: boolean;
    text?: boolean;
  }>(),
  {
    circle: true,
    text: false
  }
);

const { t } = useI18n();
const localeStore = useLocaleStore();

function handleCommand(command: string | number) {
  localeStore.setLocale(command as SupportedLocale);
}
</script>

<style scoped>
.language-switch {
  gap: 6px;
}
</style>
