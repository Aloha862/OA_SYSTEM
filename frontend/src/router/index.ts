import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { routes } from './routes';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useNotificationStore } from '@/stores/notification';
import { notificationSocket } from '@/utils/ws';
import { i18n } from '@/i18n';

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
});

router.beforeEach(async (to) => {
  const userStore = useUserStore();
  const permissionStore = usePermissionStore();
  const t = i18n.global.t;

  if (to.path === '/login' || to.path === '/register') {
    return userStore.token ? { path: '/dashboard' } : true;
  }

  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  try {
    if (!userStore.userInfo) {
      await userStore.fetchCurrentUser();
    }

    if (userStore.userInfo && permissionStore.menus.length === 0) {
      permissionStore.generateMenus(userStore.userInfo);
    }

    if (userStore.token) {
      notificationSocket.connect(userStore.token);
      useNotificationStore().fetchUnreadCount();
    }
  } catch {
    userStore.logoutLocal();
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  if (!permissionStore.canAccess(to)) {
    ElMessage.warning(t('layout.noAccess'));
    return { path: '/dashboard' };
  }

  const titleKey = to.meta.titleKey || to.meta.title;
  document.title = `${titleKey ? t(String(titleKey)) : t('app.workspace')} - ${t('app.name')}`;
  return true;
});

export default router;
