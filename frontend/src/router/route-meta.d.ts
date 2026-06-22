import 'vue-router';
import type { Role } from '@/api/types';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    titleKey?: string;
    icon?: string;
    hidden?: boolean;
    roles?: Role[];
    requiresApprover?: boolean;
  }
}
