import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '../views/LoginView.vue';
import DashboardView from '../views/DashboardView.vue';
import AnnualLeaveView from '../views/AnnualLeaveView.vue';
import LeaveRecordsView from '../views/LeaveRecordsView.vue';
import CreateLeaveView from '../views/CreateLeaveView.vue';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: DashboardView,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/annual-leave' },
        { path: 'annual-leave', name: 'annual-leave', component: AnnualLeaveView },
        { path: 'leave-records', name: 'leave-records', component: LeaveRecordsView },
        { path: 'create-leave', name: 'create-leave', component: CreateLeaveView }
      ]
    }
  ]
});

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.matched.some((record) => record.meta.requiresAuth) && !auth.isAuthenticated) {
    next({ name: 'login' });
    return;
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    next({ name: 'annual-leave' });
    return;
  }
  next();
});

export default router;
