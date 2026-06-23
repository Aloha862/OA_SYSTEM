import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import MarkdownRenderer from '../MarkdownRenderer.vue';

describe('MarkdownRenderer', () => {
  it('removes executable HTML and hardens links', () => {
    const wrapper = mount(MarkdownRenderer, {
      props: { content: '<img src=x onerror="alert(1)"> [站点](https://example.com)' }
    });
    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.html()).toContain('&lt;img');
    const link = wrapper.get('a');
    expect(link.attributes('target')).toBe('_blank');
    expect(link.attributes('rel')).toContain('noopener');
  });

  it('copies an individual code block', async () => {
    const wrapper = mount(MarkdownRenderer, { props: { content: '```js\nconst ok = true;\n```' } });
    await wrapper.get('[data-copy-code]').trigger('click');
    expect(vi.mocked(navigator.clipboard.writeText)).toHaveBeenCalledWith('const ok = true;\n');
  });
});
