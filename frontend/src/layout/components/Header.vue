<template>
  <header class="oa-header">
    <div class="oa-header__left">
      <el-button :icon="collapsed ? Expand : Fold" text @click="emit('toggle')" />
      <Breadcrumb />
    </div>

    <div class="oa-header__right">
      <LanguageSwitch />
      <AiDialog />
      <NotificationBell />
      <el-dropdown trigger="click" @command="handleCommand">
        <button class="user-chip" type="button">
          <el-avatar :size="32" :src="userStore.userInfo?.avatar">{{ initials }}</el-avatar>
          <span>{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">{{ t('layout.profile') }}</el-dropdown-item>
            <el-dropdown-item divided command="logout">{{ t('layout.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue';
import Breadcrumb from './Breadcrumb.vue';
import NotificationBell from '@/components/NotificationBell.vue';
import AiDialog from '@/components/AiDialog.vue';
import LanguageSwitch from '@/components/LanguageSwitch.vue';
import { usePermissionStore } from '@/stores/permission';
import { useUserStore } from '@/stores/user';

defineProps<{ collapsed: boolean }>();
const emit = defineEmits<{ (event: 'toggle'): void }>();

const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const { t } = useI18n();

const initials = computed(() => (userStore.userInfo?.realName || userStore.userInfo?.username || 'U').slice(0, 1));

async function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile');
    return;
  }

  if (command === 'logout') {
    await ElMessageBox.confirm(t('layout.logoutConfirm'), t('layout.logoutTitle'), { type: 'warning' });
    await userStore.logout();
    permissionStore.reset();
    router.replace('/login');
  }
}
</script>

<style scoped>
.oa-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 58px;
  padding: 0 24px 0 18px;
  background: rgba(255, 255, 255, 0.86);
  border-bottom: 1px solid var(--oa-border);
  backdrop-filter: blur(14px);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.72) inset;
}

.oa-header__left,
.oa-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.oa-header__left {
  flex: 1;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  max-width: 188px;
  padding: 0 10px 0 4px;
  border: 1px solid var(--oa-border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--oa-text);
  cursor: pointer;
  box-shadow: var(--oa-shadow-sm);
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

.user-chip:hover {
  border-color: var(--oa-border-strong);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.user-chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .oa-header {
    padding: 0 12px;
  }

  .oa-header__left {
    flex: 0 1 auto;
  }

  .oa-header__right {
    gap: 6px;
  }

  .oa-header__left :deep(.oa-breadcrumb),
  .user-chip span,
  .user-chip .el-icon {
    display: none;
  }

  .user-chip {
    width: 38px;
    padding: 0 2px;
    justify-content: center;
  }
}
</style>
