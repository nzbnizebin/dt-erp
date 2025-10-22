<template>
  <section class="panel">
    <h3>请假记录</h3>
    <form class="filter-grid" @submit.prevent="submitFilters">
      <label>
        英文姓名
        <input v-model="filters.englishName" placeholder="按英文名筛选" />
      </label>
      <label>
        类型
        <select v-model="filters.type">
          <option value="">全部</option>
          <option value="ANNUAL">年假</option>
          <option value="PERSONAL">事假</option>
          <option value="MARRIAGE">婚假</option>
          <option value="MATERNITY">产假</option>
          <option value="SICK">病假</option>
          <option value="OTHER">其他</option>
        </select>
      </label>
      <label>
        开始时间
        <input v-model="filters.start" type="datetime-local" />
      </label>
      <label>
        结束时间
        <input v-model="filters.end" type="datetime-local" />
      </label>
      <button type="submit">查询</button>
    </form>

    <table class="data-table">
      <thead>
        <tr>
          <th>姓名</th>
          <th>类型</th>
          <th>开始</th>
          <th>结束</th>
          <th>小时数</th>
          <th>创建时间</th>
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
        </tr>
        <tr v-if="!leaveRequests.length">
          <td colspan="6" class="empty">暂无记录</td>
        </tr>
      </tbody>
    </table>

    <div class="pagination">
      <button :disabled="page === 0" @click="changePage(page - 1)">上一页</button>
      <span>第 {{ page + 1 }} / {{ totalPages }} 页</span>
      <button :disabled="page + 1 >= totalPages" @click="changePage(page + 1)">下一页</button>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue';
import axios from 'axios';

const leaveRequests = ref([]);
const filters = reactive({ englishName: '', type: '', start: '', end: '' });
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

const loadLeaveRequests = async () => {
  const params = { page: page.value, size: size.value };
  if (filters.englishName) params.englishName = filters.englishName;
  if (filters.type) params.type = filters.type;
  if (filters.start) params.start = filters.start;
  if (filters.end) params.end = filters.end;
  const { data } = await axios.get('/api/leave-requests', { params });
  leaveRequests.value = data.content;
  page.value = data.page;
  size.value = data.size;
  totalElements.value = data.totalElements;
};

const submitFilters = async () => {
  page.value = 0;
  await loadLeaveRequests();
};

const changePage = async (nextPage) => {
  page.value = nextPage;
  await loadLeaveRequests();
};

const formatDate = (value) => new Date(value).toLocaleString();

onMounted(async () => {
  await loadLeaveRequests();
});
</script>

<style scoped>
.panel {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.filter-grid label {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-weight: 600;
  color: #1f2933;
}

.filter-grid input,
.filter-grid select {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
}

.filter-grid button {
  align-self: end;
  border: none;
  border-radius: 10px;
  padding: 10px 18px;
  background: #2563eb;
  color: white;
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
