<template>
  <div class="page-grid">
    <section class="panel">
      <h3>年假记录</h3>
      <div class="employee-layout">
        <ul class="employee-list">
          <li
            v-for="employee in employees"
            :key="employee.id"
            :class="{ active: selectedEmployee && selectedEmployee.id === employee.id }"
            @click="selectEmployee(employee)"
          >
            <span class="name">{{ employee.chineseName }} ({{ employee.englishName }})</span>
            <span class="date">入职时间：{{ employee.hireDate }}</span>
          </li>
        </ul>
        <div v-if="selectedEmployee" class="employee-details">
          <h4>年假配额</h4>
          <p>总配额：<strong>{{ summary.totalQuota.toFixed(2) }}</strong> 天</p>
          <p>已使用：<strong>{{ summary.usedDays.toFixed(2) }}</strong> 天</p>
          <p>剩余：<strong>{{ summary.remainingDays.toFixed(2) }}</strong> 天</p>
        </div>
        <div v-else class="employee-empty">
          请选择或创建员工以查看年假数据
        </div>
      </div>
    </section>

    <section v-if="auth.isAdmin" class="panel">
      <h3>{{ employeeForm.id ? '更新员工' : '新增员工' }}</h3>
      <form class="form-card" @submit.prevent="handleEmployeeSubmit">
        <div class="form-grid">
          <label>
            中文姓名
            <input v-model="employeeForm.chineseName" required />
          </label>
          <label>
            英文姓名
            <input v-model="employeeForm.englishName" required />
          </label>
          <label>
            入职日期
            <input v-model="employeeForm.hireDate" type="date" required />
          </label>
        </div>
        <div class="form-actions">
          <button type="submit">{{ employeeForm.id ? '保存修改' : '创建' }}</button>
          <button type="button" class="secondary" @click="resetEmployeeForm">重置</button>
          <button
            v-if="employeeForm.id"
            type="button"
            class="danger"
            @click="deleteEmployee(employeeForm.id)"
          >
            删除
          </button>
        </div>
        <p v-if="employeeError" class="error">{{ employeeError }}</p>
      </form>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const employees = ref([]);
const selectedEmployee = ref(null);
const summary = reactive({ totalQuota: 0, usedDays: 0, remainingDays: 0 });
const employeeForm = reactive({ id: null, chineseName: '', englishName: '', hireDate: '' });
const employeeError = ref('');

const loadEmployees = async () => {
  const { data } = await axios.get('/api/employees');
  employees.value = data;
  if (data.length === 0) {
    selectedEmployee.value = null;
    resetEmployeeForm();
    Object.assign(summary, { totalQuota: 0, usedDays: 0, remainingDays: 0 });
    return;
  }
  const currentId = selectedEmployee.value?.id;
  const match = data.find((emp) => emp.id === currentId) || data[0];
  await selectEmployee(match);
};

const loadSummary = async (employeeId) => {
  const { data } = await axios.get(`/api/employees/${employeeId}/annual-leave`);
  Object.assign(summary, data);
};

const selectEmployee = async (employee) => {
  selectedEmployee.value = employee;
  Object.assign(employeeForm, employee);
  employeeError.value = '';
  await loadSummary(employee.id);
};

const resetEmployeeForm = () => {
  employeeForm.id = null;
  employeeForm.chineseName = '';
  employeeForm.englishName = '';
  employeeForm.hireDate = '';
  employeeError.value = '';
};

const handleEmployeeSubmit = async () => {
  try {
    if (employeeForm.id) {
      await axios.put(`/api/employees/${employeeForm.id}`, employeeForm);
      await loadEmployees();
    } else {
      await axios.post('/api/employees', employeeForm);
      resetEmployeeForm();
      await loadEmployees();
    }
  } catch (error) {
    employeeError.value = error.response?.data?.message || '保存员工信息失败';
  }
};

const deleteEmployee = async (id) => {
  if (!confirm('确认删除该员工及其请假记录？')) return;
  try {
    await axios.delete(`/api/employees/${id}`);
    resetEmployeeForm();
    await loadEmployees();
  } catch (error) {
    employeeError.value = error.response?.data?.message || '删除失败';
  }
};

onMounted(async () => {
  await loadEmployees();
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

.employee-layout {
  display: flex;
  gap: 20px;
  min-height: 260px;
}

.employee-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 45%;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  max-height: 340px;
  overflow: auto;
}

.employee-list li {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.employee-list li:last-child {
  border-bottom: none;
}

.employee-list li.active {
  background: #eff6ff;
}

.employee-details {
  flex: 1;
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.employee-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  border: 1px dashed #cbd5f5;
  border-radius: 12px;
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

.form-grid input {
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
</style>
