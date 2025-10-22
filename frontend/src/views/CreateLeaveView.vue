<template>
  <div class="page-grid">
    <section class="panel">
      <h3>创建请假请求</h3>
      <form class="form-card" @submit.prevent="handleLeaveSubmit">
        <div class="form-grid">
          <label>
            英文姓名
            <input v-model="leaveForm.englishName" required />
          </label>
          <label>
            开始时间
            <input v-model="leaveForm.startTime" type="datetime-local" required />
          </label>
          <label>
            结束时间
            <input v-model="leaveForm.endTime" type="datetime-local" required />
          </label>
          <label>
            小时数
            <input v-model.number="leaveForm.hours" type="number" min="1" step="1" required />
          </label>
          <label>
            类型
            <select v-model="leaveForm.type" required>
              <option value="ANNUAL">年假</option>
              <option value="PERSONAL">事假</option>
              <option value="MARRIAGE">婚假</option>
              <option value="MATERNITY">产假</option>
              <option value="SICK">病假</option>
              <option value="OTHER">其他</option>
            </select>
          </label>
        </div>
        <div class="form-actions">
          <button type="submit">提交</button>
          <button type="button" class="secondary" @click="resetLeaveForm">重置</button>
        </div>
        <p v-if="leaveError" class="error">{{ leaveError }}</p>
      </form>
    </section>

    <section class="panel">
      <div class="panel-header">
        <h3>最近请假请求</h3>
        <button class="secondary" @click="refresh">刷新</button>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>姓名</th>
            <th>类型</th>
            <th>开始</th>
            <th>结束</th>
            <th>小时数</th>
            <th>创建时间</th>
            <th v-if="auth.isAdmin">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in leaveRequests" :key="record.id">
            <td>{{ record.englishName }}</td>
            <td>{{ typeLabels[record.type] || record.type }}</td>
            <td>{{ formatDate(record.startTime) }}</td>
            <td>{{ formatDate(record.endTime) }}</td>
            <td>{{ record.hours }}</td>
            <td>{{ formatDate(record.createdAt) }}</td>
            <td v-if="auth.isAdmin">
              <button class="danger" @click="deleteLeave(record.id)">删除</button>
            </td>
          </tr>
          <tr v-if="!leaveRequests.length">
            <td :colspan="auth.isAdmin ? 7 : 6" class="empty">暂无请假记录</td>
          </tr>
        </tbody>
      </table>

      <div class="pagination">
        <button :disabled="page === 0" @click="changePage(page - 1)">上一页</button>
        <span>第 {{ page + 1 }} / {{ totalPages }} 页</span>
        <button :disabled="page + 1 >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const leaveForm = reactive({ englishName: '', startTime: '', endTime: '', hours: 8, type: 'ANNUAL' });
const leaveError = ref('');
const leaveRequests = ref([]);
const page = ref(0);
const size = ref(10);
const totalElements = ref(0);

const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / size.value)));

const typeLabels = {
  ANNUAL: '年假',
  PERSONAL: '事假',
  MARRIAGE: '婚假',
  MATERNITY: '产假',
  SICK: '病假',
  OTHER: '其他'
};

const resetLeaveForm = () => {
  leaveForm.englishName = '';
  leaveForm.startTime = '';
  leaveForm.endTime = '';
  leaveForm.hours = 8;
  leaveForm.type = 'ANNUAL';
  leaveError.value = '';
};

const handleLeaveSubmit = async () => {
  try {
    await axios.post('/api/leave-requests', { ...leaveForm, hours: Number(leaveForm.hours) });
    resetLeaveForm();
    await refresh();
  } catch (error) {
    leaveError.value = error.response?.data?.message || '提交失败';
  }
};

const loadLeaveRequests = async () => {
  const params = { page: page.value, size: size.value };
  const { data } = await axios.get('/api/leave-requests', { params });
  leaveRequests.value = data.content;
  page.value = data.page;
  size.value = data.size;
  totalElements.value = data.totalElements;
};

const changePage = async (nextPage) => {
  page.value = nextPage;
  await loadLeaveRequests();
};

const refresh = async () => {
  await loadLeaveRequests();
};

const deleteLeave = async (id) => {
  if (!auth.isAdmin) return;
  if (!confirm('确认删除该请假记录？')) return;
  try {
    await axios.delete(`/api/leave-requests/${id}`);
    await refresh();
  } catch (error) {
    leaveError.value = error.response?.data?.message || '删除失败';
  }
};

const formatDate = (value) => new Date(value).toLocaleString();

onMounted(async () => {
  await loadLeaveRequests();
});
</script>

<style scoped>
.page-grid {
  display: grid;
  gap: 24px;
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.form-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 600;
  color: #1f2933;
}

.form-grid input,
.form-grid select {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
}

.form-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

button {
  border: none;
  border-radius: 10px;
  padding: 10px 18px;
  background: #2563eb;
  color: white;
  font-weight: 600;
}

button.secondary {
  background: #94a3b8;
}

button.danger {
  background: #dc2626;
}

.error {
  color: #dc2626;
  font-weight: 600;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.data-table th {
  background: #f8fafc;
  font-weight: 700;
}

.data-table .empty {
  text-align: center;
  color: #64748b;
}

.pagination {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.pagination button {
  border: none;
  border-radius: 10px;
  padding: 8px 16px;
  background: #2563eb;
  color: white;
  font-weight: 600;
}

.pagination button:disabled {
  background: #cbd5f5;
  color: #475569;
  cursor: not-allowed;
}
</style>
