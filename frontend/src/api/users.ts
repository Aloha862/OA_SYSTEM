import request from '@/utils/request';
import type { PageQuery, PageResult, UserRecord, UserInfo } from './types';

export const usersApi = {
  page(params: PageQuery) {
    return request.get<PageResult<UserRecord>>('/users/page', { params });
  },
  detail(id: number) {
    return request.get<UserRecord>(`/users/${id}`);
  },
  create(data: Partial<UserRecord> & { password?: string }) {
    return request.post<void>('/users', data);
  },
  update(id: number, data: Partial<UserRecord>) {
    return request.put<void>(`/users/${id}`, data);
  },
  remove(id: number) {
    return request.delete<void>(`/users/${id}`);
  },
  removeBatch(ids: number[]) {
    return request.delete<void>('/users/batch', { data: { ids } });
  },
  updateStatus(id: number, status: number) {
    return request.put<void>(`/users/${id}/status`, { status });
  },
  resetPassword(id: number, password = '123456') {
    return request.put<void>(`/users/${id}/password/reset`, { password });
  },
  setApprover(id: number, isApprover: boolean) {
    return request.put<void>(`/users/${id}/approver`, { approver: isApprover });
  },
  approvers() {
    return request.get<UserRecord[]>('/users/approvers');
  },
  options() {
    return request.get<UserRecord[]>('/users/options');
  },
  profile() {
    return request.get<UserInfo>('/users/profile');
  },
  updateProfile(data: Partial<UserInfo>) {
    return request.put<UserInfo>('/users/profile', data);
  }
};
