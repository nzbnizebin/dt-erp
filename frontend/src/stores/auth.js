import { defineStore } from 'pinia';
import axios from 'axios';

const TOKEN_KEY = 'attendance_token';
const ROLE_KEY = 'attendance_role';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    role: localStorage.getItem(ROLE_KEY) || ''
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.role === 'ADMIN'
  },
  actions: {
    async login(username, password) {
      const response = await axios.post('/api/auth/login', { username, password });
      this.token = response.data.token;
      this.role = response.data.role;
      localStorage.setItem(TOKEN_KEY, this.token);
      localStorage.setItem(ROLE_KEY, this.role);
      axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`;
    },
    logout() {
      this.token = '';
      this.role = '';
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ROLE_KEY);
      delete axios.defaults.headers.common['Authorization'];
    },
    initialize() {
      if (this.token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`;
      }
    }
  }
});
