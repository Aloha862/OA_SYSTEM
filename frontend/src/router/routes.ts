import type { RouteRecordRaw } from 'vue-router';
import Layout from '@/layout/index.vue';

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { titleKey: 'route.login', hidden: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { titleKey: 'route.register', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { titleKey: 'route.dashboard', icon: 'Odometer' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/index.vue'),
        meta: { titleKey: 'route.users', icon: 'User', roles: ['ADMIN'] }
      },
      {
        path: 'departments',
        name: 'Departments',
        component: () => import('@/views/department/index.vue'),
        meta: { titleKey: 'route.departments', icon: 'OfficeBuilding' }
      },
      {
        path: 'approvals',
        name: 'Approvals',
        component: () => import('@/views/approval/list.vue'),
        meta: { titleKey: 'route.approvals', icon: 'Tickets' }
      },
      {
        path: 'approvals/create',
        name: 'ApprovalCreate',
        component: () => import('@/views/approval/create.vue'),
        meta: { titleKey: 'route.approvalCreate', icon: 'EditPen' }
      },
      {
        path: 'approvals/todo',
        name: 'ApprovalTodo',
        component: () => import('@/views/approval/todo.vue'),
        meta: { titleKey: 'route.approvalTodo', icon: 'Checked', requiresApprover: true }
      },
      {
        path: 'approvals/:id',
        name: 'ApprovalDetail',
        component: () => import('@/views/approval/detail.vue'),
        meta: { titleKey: 'route.approvalDetail', hidden: true }
      },
      {
        path: 'schedules',
        name: 'Schedules',
        component: () => import('@/views/schedule/list.vue'),
        meta: { titleKey: 'route.schedules', icon: 'Calendar' }
      },
      {
        path: 'schedules/calendar',
        name: 'ScheduleCalendar',
        component: () => import('@/views/schedule/calendar.vue'),
        meta: { titleKey: 'route.scheduleCalendar', icon: 'Calendar' }
      },
      {
        path: 'news',
        name: 'News',
        component: () => import('@/views/news/list.vue'),
        meta: { titleKey: 'route.news', icon: 'Reading' }
      },
      {
        path: 'news/:id',
        name: 'NewsDetail',
        component: () => import('@/views/news/detail.vue'),
        meta: { titleKey: 'route.newsDetail', hidden: true }
      },
      {
        path: 'news-manage',
        name: 'NewsManage',
        component: () => import('@/views/news/manage.vue'),
        meta: { titleKey: 'route.newsManage', icon: 'DocumentChecked', roles: ['ADMIN'] }
      },
      {
        path: 'dicts',
        name: 'Dicts',
        component: () => import('@/views/dict/index.vue'),
        meta: { titleKey: 'route.dicts', icon: 'Collection', roles: ['ADMIN'] }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notification/index.vue'),
        meta: { titleKey: 'route.notifications', icon: 'Bell' }
      },
      {
        path: 'ai',
        name: 'Ai',
        component: () => import('@/views/ai/index.vue'),
        meta: { titleKey: 'route.ai', icon: 'MagicStick', standalone: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { titleKey: 'route.profile', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
    meta: { hidden: true }
  }
];

export const layoutChildren = routes.find((route) => route.path === '/')?.children || [];
