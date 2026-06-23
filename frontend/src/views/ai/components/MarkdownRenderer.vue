<template><div class="markdown-body" v-html="html" @click="handleClick" /></template>

<script setup lang="ts">
import { computed } from 'vue';
import MarkdownIt from 'markdown-it';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import java from 'highlight.js/lib/languages/java';
import json from 'highlight.js/lib/languages/json';
import sql from 'highlight.js/lib/languages/sql';
import bash from 'highlight.js/lib/languages/bash';
import xml from 'highlight.js/lib/languages/xml';
import css from 'highlight.js/lib/languages/css';
import 'highlight.js/styles/github.css';

const props = defineProps<{ content: string }>();
hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('js', javascript);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('ts', typescript);
hljs.registerLanguage('java', java);
hljs.registerLanguage('json', json);
hljs.registerLanguage('sql', sql);
hljs.registerLanguage('bash', bash);
hljs.registerLanguage('shell', bash);
hljs.registerLanguage('html', xml);
hljs.registerLanguage('xml', xml);
hljs.registerLanguage('css', css);
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code, language) {
    if (language && hljs.getLanguage(language)) return hljs.highlight(code, { language }).value;
    return hljs.highlightAuto(code).value;
  }
});
const defaultFence = md.renderer.rules.fence!;
md.renderer.rules.fence = (tokens, index, options, env, self) => {
  const language = tokens[index].info.trim().split(/\s+/)[0] || 'text';
  return `<div class="code-block"><div class="code-toolbar"><span>${md.utils.escapeHtml(language)}</span><button type="button" data-copy-code>复制代码</button></div>${defaultFence(tokens, index, options, env, self)}</div>`;
};
const defaultLinkOpen = md.renderer.rules.link_open || ((tokens, index, options, env, self) => self.renderToken(tokens, index, options));
md.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet('target', '_blank');
  tokens[index].attrSet('rel', 'noopener noreferrer');
  return defaultLinkOpen(tokens, index, options, env, self);
};
const html = computed(() => DOMPurify.sanitize(md.render(props.content || ''), {
  ADD_ATTR: ['target']
}));
async function handleClick(event: MouseEvent) {
  const button = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-copy-code]');
  if (!button) return;
  const code = button.closest('.code-block')?.querySelector('code')?.textContent || '';
  await navigator.clipboard.writeText(code);
  button.textContent = '已复制';
  window.setTimeout(() => (button.textContent = '复制代码'), 1400);
}
</script>

<style scoped>
.markdown-body { color: #18212f; font-size: 15px; line-height: 1.78; overflow-wrap: anywhere; }
.markdown-body :deep(p) { margin: 0 0 12px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin: 24px 0 10px; line-height: 1.35; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 24px; }
.markdown-body :deep(blockquote) { margin: 14px 0; padding: 2px 0 2px 16px; border-left: 3px solid #9ab8ff; color: #536174; }
.markdown-body :deep(pre) { margin: 16px 0; padding: 16px; border-radius: 12px; background: #111827; overflow: auto; }
.markdown-body :deep(.code-block) { margin: 16px 0; overflow: hidden; border-radius: 12px; background: #111827; }
.markdown-body :deep(.code-toolbar) { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: #1f2937; color: #9ca3af; font-size: 12px; }
.markdown-body :deep(.code-toolbar button) { border: 0; background: transparent; color: #dbeafe; cursor: pointer; }
.markdown-body :deep(.code-block pre) { margin: 0; border-radius: 0; }
.markdown-body :deep(pre code) { color: #e5edf8; background: transparent; font-size: 13px; }
.markdown-body :deep(code) { padding: 2px 6px; border-radius: 5px; background: #eef3fa; font-family: 'SFMono-Regular', Consolas, monospace; font-size: .9em; }
.markdown-body :deep(a) { color: #245eea; }
.markdown-body :deep(table) { width: 100%; border-collapse: collapse; }
.markdown-body :deep(th), .markdown-body :deep(td) { padding: 8px 10px; border: 1px solid #dce4ef; text-align: left; }
</style>
