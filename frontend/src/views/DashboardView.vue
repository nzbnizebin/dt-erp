<template>
  <div class="layout">
    <header class="top-bar">
      <h2>Attendance Dashboard</h2>
      <div class="user-actions">
        <span class="role-badge">{{ auth.role }}</span>
        <button @click="logout">Logout</button>
      </div>
    </header>
    <nav class="nav-bar">
      <RouterLink to="/annual-leave" class="nav-link" :class="{ active: route.name === 'annual-leave' }">
        年假记录
      </RouterLink>
      <RouterLink to="/leave-records" class="nav-link" :class="{ active: route.name === 'leave-records' }">
        请假记录
      </RouterLink>
      <RouterLink to="/create-leave" class="nav-link" :class="{ active: route.name === 'create-leave' }">
        创建请假
      </RouterLink>
    </nav>
    <main class="content">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const logout = () => {
  auth.logout();
  router.push('/login');
};
</script>

<style scoped>
.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-bar {
  background: white;
  padding: 16px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.08);
}

.role-badge {
  background: #2563eb;
  color: white;
  padding: 6px 12px;
  border-radius: 999px;
  margin-right: 12px;
}

.user-actions button {
  background: #334155;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
}

.nav-bar {
  display: flex;
  gap: 16px;
  padding: 12px 32px;
  background: #1e293b;
}

.nav-link {
  color: rgba(255, 255, 255, 0.7);
  padding: 8px 18px;
  border-radius: 999px;
  font-weight: 600;
}

.nav-link.active {
  background: #3b82f6;
  color: white;
}

.content {
  flex: 1;
  padding: 32px;
  background: #f2f4f7;
}
</style>
