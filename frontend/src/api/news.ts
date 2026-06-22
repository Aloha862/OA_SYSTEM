import request from '@/utils/request';
import type { NewsComment, NewsRecord, PageQuery, PageResult } from './types';

export const newsApi = {
  page(params: PageQuery) {
    return request.get<PageResult<NewsRecord>>('/news/page', { params });
  },
  detail(id: number) {
    return request.get<NewsRecord>(`/news/${id}`);
  },
  create(data: Partial<NewsRecord>) {
    return request.post<NewsRecord>('/news', data);
  },
  update(id: number, data: Partial<NewsRecord>) {
    return request.put<void>(`/news/${id}`, data);
  },
  remove(id: number) {
    return request.delete<void>(`/news/${id}`);
  },
  publish(id: number) {
    return request.post<void>(`/news/${id}/publish`);
  },
  offline(id: number) {
    return request.post<void>(`/news/${id}/offline`);
  },
  top(id: number, isTop: boolean) {
    return request.post<void>(`/news/${id}/top`, { isTop });
  },
  view(id: number) {
    return request.post<void>(`/news/${id}/view`);
  },
  comments(id: number) {
    return request.get<NewsComment[]>(`/news/${id}/comments`);
  },
  addComment(id: number, content: string, parentId = 0) {
    return request.post<NewsComment>(`/news/${id}/comments`, { content, parentId });
  },
  removeComment(commentId: number) {
    return request.delete<void>(`/news/comments/${commentId}`);
  },
  like(id: number) {
    return request.post<void>(`/news/${id}/like`);
  },
  unlike(id: number) {
    return request.delete<void>(`/news/${id}/like`);
  },
  favorite(id: number) {
    return request.post<void>(`/news/${id}/favorite`);
  },
  unfavorite(id: number) {
    return request.delete<void>(`/news/${id}/favorite`);
  },
  myFavorites() {
    return request.get<NewsRecord[]>('/news/favorites/my');
  },
  aiGenerate(data: { topic: string; keywords?: string; tone?: string; wordCount?: number; category?: string }) {
    return request.post<Partial<NewsRecord>>('/news/ai-generate', data);
  },
  aiPolish(data: { title: string; content: string; style?: string }) {
    return request.post<Partial<NewsRecord>>('/news/ai-polish', data);
  }
};
