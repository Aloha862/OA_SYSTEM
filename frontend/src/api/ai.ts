import request from '@/utils/request';
import type { AiConversation, AiMessage, AiResponse, PageQuery, PageResult } from './types';
import { getToken } from '@/utils/auth';

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
  },
  conversations() {
    return request.get<AiConversation[]>('/ai/v2/conversations');
  },
  createConversation(data: { title?: string; mode?: string } = {}) {
    return request.post<AiConversation>('/ai/v2/conversations', data);
  },
  renameConversation(id: number, title: string) {
    return request.patch<AiConversation>(`/ai/v2/conversations/${id}`, { title });
  },
  removeConversation(id: number) {
    return request.delete(`/ai/v2/conversations/${id}`);
  },
  clearConversation(id: number) {
    return request.delete(`/ai/v2/conversations/${id}/messages`);
  },
  messages(id: number) {
    return request.get<AiMessage[]>(`/ai/v2/conversations/${id}/messages`);
  }
};

export interface AiStreamCallbacks {
  onMeta?: (data: Record<string, unknown>) => void;
  onDelta?: (content: string) => void;
  onDone?: (data: Record<string, unknown>) => void;
  onError?: (data: { code?: string; message?: string; retryable?: boolean }) => void;
}

export async function streamAiMessage(
  conversationId: number,
  content: string,
  clientMessageId: string,
  callbacks: AiStreamCallbacks,
  signal?: AbortSignal
) {
  return consumeAiStream(`/api/ai/v2/conversations/${conversationId}/messages/stream`, callbacks, signal, {
    content,
    clientMessageId
  });
}

export async function regenerateAiMessage(
  conversationId: number,
  messageId: number,
  callbacks: AiStreamCallbacks,
  signal?: AbortSignal
) {
  return consumeAiStream(`/api/ai/v2/conversations/${conversationId}/messages/${messageId}/regenerate`, callbacks, signal);
}

async function consumeAiStream(url: string, callbacks: AiStreamCallbacks, signal?: AbortSignal, body?: object) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`
    },
    body: body ? JSON.stringify(body) : undefined,
    signal
  });
  if (!response.ok || !response.body) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message || `请求失败 (${response.status})`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n');
    let boundary = buffer.indexOf('\n\n');
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      const event = block.match(/^event:\s*(.+)$/m)?.[1]?.trim();
      const dataText = block.match(/^data:\s*(.+)$/m)?.[1];
      if (event && dataText) {
        const data = JSON.parse(dataText);
        if (event === 'meta') callbacks.onMeta?.(data);
        if (event === 'delta') callbacks.onDelta?.(String(data.content || ''));
        if (event === 'done') callbacks.onDone?.(data);
        if (event === 'error') callbacks.onError?.(data);
      }
      boundary = buffer.indexOf('\n\n');
    }
    if (done) break;
  }
}
