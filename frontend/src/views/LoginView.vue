<template>
  <div class="auth-wrapper">
    <form class="auth-card" @submit.prevent="handleSubmit">
      <h1>Attendance System</h1>
      <label>
        Username
        <input v-model="form.username" type="text" required autocomplete="username" />
      </label>
      <label>
        Password
        <input v-model="form.password" type="password" required autocomplete="current-password" />
      </label>
      <button type="submit">Login</button>
      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const auth = useAuthStore();
const form = reactive({ username: '', password: '' });
const error = ref('');

const handleSubmit = async () => {
  error.value = '';
  try {
    await auth.login(form.username, form.password);
    router.push('/');
  } catch (err) {
    error.value = err.response?.data?.message || 'Login failed';
  }
};
</script>

<style scoped>
.auth-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-card {
  width: 320px;
  background: white;
  padding: 32px;
  border-radius: 12px;
  box-shadow: 0 10px 35px rgba(15, 23, 42, 0.15);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-card h1 {
  text-align: center;
  margin-bottom: 8px;
}

label {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  gap: 8px;
}

input {
  padding: 10px 12px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  font-size: 16px;
}

button {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 12px;
  font-size: 16px;
}

.error {
  color: #b91c1c;
  text-align: center;
}
</style>
