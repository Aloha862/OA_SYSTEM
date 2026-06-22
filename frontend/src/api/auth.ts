import request from '@/utils/request';
import type { LoginRequest, LoginResponse, RegisterRequest, UserInfo } from './types';

export const authApi = {
  login(data: LoginRequest) {
    return request.post<LoginResponse>('/auth/login', data);
  },
  register(data: RegisterRequest) {
    return request.post<UserInfo>('/auth/register', data);
  },
  logout() {
    return request.post<void>('/auth/logout');
  },
  me() {
    return request.get<UserInfo>('/auth/me');
  },
  refresh() {
    return request.post<{ token: string }>('/auth/refresh');
  }
};
