import request from '@/utils/request';
import type { Department, PageQuery, PageResult } from './types';

export const departmentsApi = {
  tree() {
    return request.get<Department[]>('/departments/tree');
  },
  page(params: PageQuery) {
    return request.get<PageResult<Department>>('/departments/page', { params });
  },
  detail(id: number) {
    return request.get<Department>(`/departments/${id}`);
  },
  create(data: Partial<Department>) {
    return request.post<void>('/departments', data);
  },
  update(id: number, data: Partial<Department>) {
    return request.put<void>(`/departments/${id}`, data);
  },
  remove(id: number) {
    return request.delete<void>(`/departments/${id}`);
  },
  updateLeader(id: number, leaderId: number) {
    return request.put<void>(`/departments/${id}/leader`, { userId: leaderId });
  },
  updateApprover(id: number, approverId: number) {
    return request.put<void>(`/departments/${id}/approver`, { userId: approverId });
  }
};
