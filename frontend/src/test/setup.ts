import { vi } from 'vitest';
import { config } from '@vue/test-utils';

config.global.stubs = {
  'el-icon': true
};

Object.defineProperty(navigator, 'clipboard', {
  configurable: true,
  value: { writeText: vi.fn().mockResolvedValue(undefined) }
});
