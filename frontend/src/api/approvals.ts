import request from '@/utils/request';
import type { ApprovalFlowRecord, ApprovalRecord, PageQuery, PageResult } from './types';

export const approvalsApi = {
  page(params: PageQuery) {
    return request.get<PageResult<ApprovalRecord>>('/approvals/page', { params });
  },
  my(params: PageQuery) {
    return request.get<PageResult<ApprovalRecord>>('/approvals/my', { params });
  },
  todo(params: PageQuery) {
    return request.get<PageResult<ApprovalRecord>>('/approvals/todo', { params });
  },
  done(params: PageQuery) {
    return request.get<PageResult<ApprovalRecord>>('/approvals/done', { params });
  },
  detail(id: number) {
    return request.get<ApprovalRecord>(`/approvals/${id}`);
  },
  create(data: Partial<ApprovalRecord>) {
    return request.post<ApprovalRecord>('/approvals', data);
  },
  update(id: number, data: Partial<ApprovalRecord>) {
    return request.put<void>(`/approvals/${id}`, data);
  },
  remove(id: number) {
    return request.delete<void>(`/approvals/${id}`);
  },
  submit(id: number) {
    return request.post<void>(`/approvals/${id}/submit`);
  },
  withdraw(id: number) {
    return request.post<void>(`/approvals/${id}/withdraw`);
  },
  approve(id: number, comment: string) {
    return request.post<void>(`/approvals/${id}/approve`, { comment });
  },
  reject(id: number, comment: string) {
    return request.post<void>(`/approvals/${id}/reject`, { comment });
  },
  records(id: number) {
    return request.get<ApprovalFlowRecord[]>(`/approvals/${id}/records`);
  },
  aiSummary(id: number) {
    return request.post<{ summary: string }>(`/approvals/${id}/ai-summary`);
  },
  aiRisk(id: number) {
    return request.post<{ riskLevel: string; suggestion: string; reason?: string }>(`/approvals/${id}/ai-risk`);
  }
};
