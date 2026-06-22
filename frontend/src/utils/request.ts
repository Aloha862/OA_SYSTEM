import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { clearToken, getToken } from '@/utils/auth';
import type { Result } from '@/api/types';

interface OaRequestInstance extends AxiosInstance {
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>;
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>;
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>;
  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>;
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>;
}

const service = axios.create({
  baseURL: '/api',
  timeout: 20000
});

function normalizePageParams(params: unknown) {
  if (!params || typeof params !== 'object' || params instanceof URLSearchParams) {
    return params;
  }

  const normalized = { ...(params as Record<string, unknown>) };
  if (normalized.pageNum !== undefined && normalized.current === undefined) {
    normalized.current = normalized.pageNum;
  }
  if (normalized.pageSize !== undefined && normalized.size === undefined) {
    normalized.size = normalized.pageSize;
  }
  delete normalized.pageNum;
  delete normalized.pageSize;
  return normalized;
}

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  config.params = normalizePageParams(config.params);
  return config;
});

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data;
    }

    const payload = response.data as Result<unknown>;
    if (!payload || typeof payload.code === 'undefined') {
      return response.data;
    }

    if (payload.code === 200) {
      return payload.data;
    }

    if (payload.code === 401) {
      clearToken();
      if (router.currentRoute.value.path !== '/login') {
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
      }
    }

    if (payload.code === 403) {
      ElMessage.warning(payload.message || '无权限访问该资源');
    } else {
      ElMessage.error(payload.message || '请求处理失败');
    }

    return Promise.reject(new Error(payload.message || '请求处理失败'));
  },
  (error: AxiosError<Result<unknown>>) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || '网络连接异常';

    if (status === 401) {
      clearToken();
      router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } });
    } else if (status === 403) {
      ElMessage.warning('无权限访问该资源');
    } else {
      ElMessage.error(message);
    }

    return Promise.reject(error);
  }
);

export default service as OaRequestInstance;
