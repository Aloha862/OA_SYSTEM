import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { authApi } from '@/api/auth';
import { usersApi } from '@/api/users';
import type { LoginRequest, UserInfo } from '@/api/types';
import { clearRememberedUsername, clearToken, getToken, setRememberedUsername, setToken } from '@/utils/auth';
import { notificationSocket } from '@/utils/ws';

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken());
  const userInfo = ref<UserInfo | null>(null);

  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN');
  const isApprover = computed(() => isAdmin.value || userInfo.value?.isApprover === true || userInfo.value?.isApprover === 1);

  async function login(data: LoginRequest, remember = false) {
    const result = await authApi.login(data);
    token.value = result.token;
    userInfo.value = result.userInfo;
    setToken(result.token, remember);
    if (remember) {
      setRememberedUsername(data.username);
    } else {
      clearRememberedUsername();
    }
    notificationSocket.connect(result.token);
    return result;
  }

  async function fetchCurrentUser() {
    const result = await authApi.me();
    userInfo.value = result;
    return result;
  }

  async function fetchProfile() {
    const result = await usersApi.profile();
    userInfo.value = { ...userInfo.value, ...result };
    return result;
  }

  function setUserInfo(user: UserInfo) {
    userInfo.value = { ...userInfo.value, ...user };
  }

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      logoutLocal();
    }
  }

  function logoutLocal() {
    token.value = '';
    userInfo.value = null;
    clearToken();
    notificationSocket.disconnect();
  }

  function hasPermission(code: string) {
    if (isAdmin.value) return true;
    return Boolean(userInfo.value?.permissions?.includes(code));
  }

  return {
    token,
    userInfo,
    isAdmin,
    isApprover,
    login,
    fetchCurrentUser,
    fetchProfile,
    setUserInfo,
    logout,
    logoutLocal,
    hasPermission
  };
});
