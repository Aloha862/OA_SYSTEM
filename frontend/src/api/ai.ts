import request from '@/utils/request';
import type { AiResponse, PageQuery, PageResult } from './types';

export const aiApi = {
  approvalSummary(data: unknown) {
    return request.post<AiResponse>('/ai/approval-summary', data);
  },
  approvalRisk(data: unknown) {
    return request.post<AiResponse>('/ai/approval-risk', data);
  },
  newsGenerate(data: unknown) {
    return request.post<AiResponse>('/ai/news-generate', data);
  },
  newsPolish(data: unknown) {
    return request.post<AiResponse>('/ai/news-polish', data);
  },
  scheduleParse(data: { text: string }) {
    return request.post<AiResponse>('/ai/schedule-parse', data);
  },
  qa(data: { question: string }) {
    return request.post<AiResponse>('/ai/qa', data);
  },
  logs(params: PageQuery) {
    return request.get<PageResult<Record<string, unknown>>>('/ai/logs/page', { params });
  }
};
