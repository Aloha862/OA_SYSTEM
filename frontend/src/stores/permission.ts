import { ref } from 'vue';
import { defineStore } from 'pinia';
import type { RouteLocationNormalized, RouteMeta, RouteRecordRaw } from 'vue-router';
import type { UserInfo } from '@/api/types';
import { layoutChildren } from '@/router/routes';

export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<RouteRecordRaw[]>([]);
  const currentUser = ref<UserInfo | null>(null);

  function isApprover(user: UserInfo) {
    return user.role === 'ADMIN' || user.isApprover === true || user.isApprover === 1;
  }

  function canAccessMeta(meta: RouteMeta = {}, user = currentUser.value) {
    if (!user) return false;
    if (meta.roles?.length && !meta.roles.includes(user.role)) return false;
    if (meta.requiresApprover && !isApprover(user)) return false;
    return true;
  }

  function generateMenus(user: UserInfo) {
    currentUser.value = user;
    menus.value = layoutChildren.filter((route) => !route.meta?.hidden && canAccessMeta(route.meta, user));
  }

  function canAccess(route: RouteLocationNormalized) {
    return canAccessMeta(route.meta);
  }

  function reset() {
    menus.value = [];
    currentUser.value = null;
  }

  return {
    menus,
    generateMenus,
    canAccess,
    canAccessMeta,
    reset
  };
});
