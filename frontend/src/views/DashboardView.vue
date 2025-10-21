<template>
  <div class="layout">
    <header class="top-bar">
      <h2>Attendance Dashboard</h2>
      <div class="user-actions">
        <span class="role-badge">{{ auth.role }}</span>
        <button @click="logout">Logout</button>
      </div>
    </header>

    <main class="content">
      <section class="panel">
        <h3>Employees</h3>
        <div class="employee-section">
          <ul class="employee-list">
            <li
              v-for="employee in employees"
              :key="employee.id"
              :class="{ active: selectedEmployee && selectedEmployee.id === employee.id }"
              @click="selectEmployee(employee)"
            >
              <span class="name">{{ employee.chineseName }} ({{ employee.englishName }})</span>
              <span class="date">Hired: {{ employee.hireDate }}</span>
            </li>
          </ul>
          <div class="employee-details" v-if="selectedEmployee">
            <h4>Annual Leave</h4>
            <p>Total: <strong>{{ leaveSummary.totalQuotaDays }}</strong> days</p>
            <p>Used: <strong>{{ leaveSummary.usedDays }}</strong> days</p>
            <p>Remaining: <strong>{{ leaveSummary.remainingDays }}</strong> days</p>
          </div>
        </div>

        <div v-if="auth.isAdmin" class="form-card">
          <h4>Add / Update Employee</h4>
          <form @submit.prevent="handleEmployeeSubmit">
            <div class="form-grid">
              <label>
                Chinese Name
                <input v-model="employeeForm.chineseName" required />
              </label>
              <label>
                English Name
                <input v-model="employeeForm.englishName" required />
              </label>
              <label>
                Hire Date
                <input v-model="employeeForm.hireDate" type="date" required />
              </label>
            </div>
            <div class="form-actions">
              <button type="submit">{{ employeeForm.id ? 'Update' : 'Create' }}</button>
              <button type="button" class="secondary" @click="resetEmployeeForm">Clear</button>
              <button
                v-if="employeeForm.id"
                type="button"
                class="danger"
                @click="deleteEmployee(employeeForm.id)"
              >
                Delete
              </button>
            </div>
            <p v-if="employeeError" class="error">{{ employeeError }}</p>
          </form>
        </div>
      </section>

      <section class="panel">
        <h3>Create Leave Request</h3>
        <form class="form-card" @submit.prevent="handleLeaveSubmit">
          <div class="form-grid">
            <label>
              English Name
              <input v-model="leaveForm.englishName" required />
            </label>
            <label>
              Start Time
              <input v-model="leaveForm.startTime" type="datetime-local" required />
            </label>
            <label>
              End Time
              <input v-model="leaveForm.endTime" type="datetime-local" required />
            </label>
            <label>
              Hours
              <input v-model.number="leaveForm.hours" type="number" min="1" step="1" required />
            </label>
            <label>
              Type
              <select v-model="leaveForm.type" required>
                <option value="ANNUAL">Annual Leave</option>
                <option value="PERSONAL">Personal Leave</option>
                <option value="MARRIAGE">Marriage Leave</option>
                <option value="MATERNITY">Maternity Leave</option>
                <option value="SICK">Sick Leave</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
          </div>
          <div class="form-actions">
            <button type="submit">Submit</button>
            <button type="button" class="secondary" @click="resetLeaveForm">Reset</button>
          </div>
          <p v-if="leaveError" class="error">{{ leaveError }}</p>
        </form>

        <div class="panel">
          <h3>Leave Records</h3>
          <form class="filter-grid" @submit.prevent="loadLeaveRequests">
            <label>
              English Name
              <input v-model="filters.englishName" />
            </label>
            <label>
              Type
              <select v-model="filters.type">
                <option value="">All</option>
                <option value="ANNUAL">Annual</option>
                <option value="PERSONAL">Personal</option>
                <option value="MARRIAGE">Marriage</option>
                <option value="MATERNITY">Maternity</option>
                <option value="SICK">Sick</option>
                <option value="OTHER">Other</option>
              </select>
            </label>
            <label>
              Start
              <input v-model="filters.start" type="datetime-local" />
            </label>
            <label>
              End
              <input v-model="filters.end" type="datetime-local" />
            </label>
            <button type="submit">Search</button>
          </form>

          <table class="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Type</th>
                <th>Start</th>
                <th>End</th>
                <th>Hours</th>
                <th v-if="auth.isAdmin">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in leaveRequests" :key="record.id">
                <td>{{ record.englishName }}</td>
                <td>{{ record.type }}</td>
                <td>{{ formatDate(record.startTime) }}</td>
                <td>{{ formatDate(record.endTime) }}</td>
                <td>{{ record.hours }}</td>
                <td v-if="auth.isAdmin">
                  <button class="danger" @click="deleteLeave(record.id)">Delete</button>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="pagination">
            <button :disabled="page === 0" @click="changePage(page - 1)">Previous</button>
            <span>Page {{ page + 1 }} of {{ totalPages }}</span>
            <button :disabled="page + 1 >= totalPages" @click="changePage(page + 1)">Next</button>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const employees = ref([]);
const selectedEmployee = ref(null);
const leaveSummary = reactive({ totalQuotaDays: 0, usedDays: 0, remainingDays: 0 });
const leaveRequests = ref([]);
const totalElements = ref(0);
const pageSize = ref(10);
const page = ref(0);
const employeeError = ref('');
const leaveError = ref('');

const employeeForm = reactive({ id: null, chineseName: '', englishName: '', hireDate: '' });
const leaveForm = reactive({ englishName: '', startTime: '', endTime: '', hours: 8, type: 'ANNUAL' });
const filters = reactive({ englishName: '', type: '', start: '', end: '' });

const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)));

const loadEmployees = async () => {
  const { data } = await axios.get('/api/employees');
  employees.value = data;
  if (!selectedEmployee.value && data.length) {
    selectEmployee(data[0]);
  }
};

const selectEmployee = async (employee) => {
  selectedEmployee.value = employee;
  Object.assign(employeeForm, employee);
  const { data } = await axios.get(`/api/employees/${employee.id}/annual-leave`);
  Object.assign(leaveSummary, data);
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
      const { data } = await axios.put(`/api/employees/${employeeForm.id}`, employeeForm);
      await loadEmployees();
      await selectEmployee(data);
    } else {
      await axios.post('/api/employees', employeeForm);
      resetEmployeeForm();
      await loadEmployees();
    }
  } catch (err) {
    employeeError.value = err.response?.data?.message || 'Unable to save employee';
  }
};

const deleteEmployee = async (id) => {
  if (!confirm('Delete this employee?')) return;
  try {
    await axios.delete(`/api/employees/${id}`);
    resetEmployeeForm();
    await loadEmployees();
  } catch (err) {
    employeeError.value = err.response?.data?.message || 'Unable to delete employee';
  }
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
    await axios.post('/api/leave-requests', {
      ...leaveForm,
      hours: Number(leaveForm.hours)
    });
    resetLeaveForm();
    await loadLeaveRequests();
    if (selectedEmployee.value) {
      await selectEmployee(selectedEmployee.value);
    }
  } catch (err) {
    leaveError.value = err.response?.data?.message || 'Unable to submit leave';
  }
};

const loadLeaveRequests = async () => {
  const params = {
    page: page.value,
    size: pageSize.value
  };
  if (filters.englishName) params.englishName = filters.englishName;
  if (filters.type) params.type = filters.type;
  if (filters.start) params.start = filters.start;
  if (filters.end) params.end = filters.end;
  const { data } = await axios.get('/api/leave-requests', { params });
  leaveRequests.value = data.content;
  page.value = data.pageNumber;
  pageSize.value = data.pageSize;
  totalElements.value = data.totalElements;
};

const changePage = async (nextPage) => {
  page.value = nextPage;
  await loadLeaveRequests();
};

const deleteLeave = async (id) => {
  if (!confirm('Delete this leave record?')) return;
  try {
    await axios.delete(`/api/leave-requests/${id}`);
    await loadLeaveRequests();
    if (selectedEmployee.value) {
      await selectEmployee(selectedEmployee.value);
    }
  } catch (err) {
    leaveError.value = err.response?.data?.message || 'Unable to delete leave record';
  }
};

const logout = () => {
  auth.logout();
  window.location.href = '/login';
};

const formatDate = (value) => new Date(value).toLocaleString();

onMounted(async () => {
  await Promise.all([loadEmployees(), loadLeaveRequests()]);
});
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

.content {
  display: grid;
  gap: 24px;
  padding: 24px 32px;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
}

.panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.employee-section {
  display: flex;
  gap: 16px;
}

.employee-list {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 50%;
  max-height: 320px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.employee-list li {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.employee-list li:last-child {
  border-bottom: none;
}

.employee-list li.active {
  background: #eff6ff;
}

.employee-details {
  flex: 1;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.form-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.form-card label {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  gap: 8px;
}

.form-card input,
.form-card select {
  padding: 10px 12px;
  border: 1px solid #cbd5f5;
  border-radius: 8px;
  font-size: 15px;
}

.form-actions {
  display: flex;
  gap: 12px;
}

.form-actions button {
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 15px;
}

.form-actions .secondary {
  background: #e2e8f0;
  color: #0f172a;
}

.form-actions .danger {
  background: #dc2626;
  color: white;
}

button.danger {
  background: #dc2626;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  align-items: end;
}

.filter-grid label {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  gap: 6px;
}

.filter-grid input,
.filter-grid select {
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.filter-grid button {
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.pagination button {
  border: none;
  border-radius: 8px;
  padding: 8px 12px;
  background: #2563eb;
  color: white;
}

.error {
  color: #b91c1c;
}
</style>
