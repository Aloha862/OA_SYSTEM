<template>
  <aside class="oa-sidebar" :class="{ collapsed }">
    <div class="oa-sidebar__brand">
      <span class="brand-mark">OA</span>
      <strong v-if="!collapsed">{{ t('app.name') }}</strong>
    </div>

    <el-menu
      class="oa-sidebar__menu"
      :default-active="route.path"
      :collapse="collapsed"
      :collapse-transition="false"
      router
    >
      <el-menu-item v-for="menu in permissionStore.menus" :key="menu.path" :index="normalizePath(menu.path)">
        <el-icon>
          <component :is="resolveIcon(menu.meta?.icon)" />
        </el-icon>
        <span>{{ menu.meta?.titleKey ? t(menu.meta.titleKey) : menu.meta?.title }}</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import {
  Bell,
  Calendar,
  Checked,
  Collection,
  DocumentChecked,
  EditPen,
  MagicStick,
  Odometer,
  OfficeBuilding,
  Reading,
  Tickets,
  User
} from '@element-plus/icons-vue';
import { usePermissionStore } from '@/stores/permission';

defineProps<{ collapsed: boolean }>();

const route = useRoute();
const permissionStore = usePermissionStore();
const { t } = useI18n();

const iconMap = {
  Bell,
  Calendar,
  Checked,
  Collection,
  DocumentChecked,
  EditPen,
  MagicStick,
  Odometer,
  OfficeBuilding,
  Reading,
  Tickets,
  User
};

function resolveIcon(name?: string) {
  return iconMap[name as keyof typeof iconMap] || Odometer;
}

function normalizePath(path: string) {
  return path.startsWith('/') ? path : `/${path}`;
}
</script>

<style scoped>
.oa-sidebar {
  width: 236px;
  flex: 0 0 236px;
  min-height: 100vh;
  background:
    linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-right: 1px solid var(--oa-border);
  transition: width 0.18s ease;
  box-shadow: 8px 0 28px rgba(15, 23, 42, 0.03);
}

.oa-sidebar.collapsed {
  width: 68px;
  flex-basis: 68px;
}

.oa-sidebar__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 58px;
  padding: 0 16px;
  border-bottom: 1px solid var(--oa-border);
  white-space: nowrap;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--oa-primary), #0f9f8f);
  color: #ffffff;
  font-weight: 760;
}

.oa-sidebar__menu {
  border-right: 0;
  padding: 10px 8px;
}

:deep(.el-menu-item) {
  height: 44px;
  margin: 4px 0;
  border-radius: 7px;
  color: #273449;
  transition:
    background 0.16s ease,
    color 0.16s ease,
    transform 0.16s ease;
}

:deep(.el-menu-item:hover) {
  background: #f6f9fd;
  transform: translateX(2px);
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, var(--oa-primary-soft), #f4fbfa);
  color: var(--oa-primary);
  font-weight: 650;
  box-shadow: inset 3px 0 0 var(--oa-primary);
}

@media (max-width: 900px) {
  .oa-sidebar {
    width: 56px;
    flex-basis: 56px;
  }

  .oa-sidebar__brand {
    justify-content: center;
    padding: 0 6px;
  }

  .brand-mark {
    width: 32px;
    height: 32px;
  }

  .oa-sidebar__brand strong,
  :deep(.el-menu-item span) {
    display: none;
  }

  :deep(.el-menu-item) {
    justify-content: center;
    padding: 0;
  }
}
</style>
