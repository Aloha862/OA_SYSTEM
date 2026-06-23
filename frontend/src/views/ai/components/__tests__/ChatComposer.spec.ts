import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ChatComposer from '../ChatComposer.vue';

describe('ChatComposer', () => {
  it('sends with Enter and keeps Shift+Enter for a new line', async () => {
    const wrapper = mount(ChatComposer, { props: { loading: false } });
    const textarea = wrapper.get('textarea');
    await textarea.setValue('  请解释审批流程  ');
    await textarea.trigger('keydown', { key: 'Enter', shiftKey: false });
    expect(wrapper.emitted('send')?.[0]).toEqual(['请解释审批流程']);
    expect((textarea.element as HTMLTextAreaElement).value).toBe('');

    await textarea.setValue('第一行');
    await textarea.trigger('keydown', { key: 'Enter', shiftKey: true });
    expect(wrapper.emitted('send')).toHaveLength(1);
  });

  it('shows stop control while streaming', async () => {
    const wrapper = mount(ChatComposer, { props: { loading: true } });
    await wrapper.get('[aria-label="停止生成"]').trigger('click');
    expect(wrapper.emitted('stop')).toHaveLength(1);
  });
});
