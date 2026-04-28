import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/auth/me'),
};

export const transactionService = {
  getAll: () => api.get('/transactions'),
  getById: (id) => api.get(`/transactions/${id}`),
  getByType: (type) => api.get(`/transactions/type/${type}`),
  getByDateRange: (start, end) => api.get(`/transactions/date-range?startDate=${start}&endDate=${end}`),
  getByMonth: (year, month) => api.get(`/transactions/month/${year}/${month}`),
  getByWeek: (date) => api.get(`/transactions/week?date=${date}`),
  create: (data) => api.post('/transactions', data),
  update: (id, data) => api.put(`/transactions/${id}`, data),
  delete: (id) => api.delete(`/transactions/${id}`),
};

export const categoryService = {
  getAll: () => api.get('/categories'),
  getByType: (type) => api.get(`/categories/type/${type}`),
  getById: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  delete: (id) => api.delete(`/categories/${id}`),
};

export const budgetService = {
  getCurrent: () => api.get('/budgets/current'),
  getByMonth: (month) => api.get(`/budgets/${month}`),
  create: (data) => api.post('/budgets', data),
  getDailyRemaining: (month) => api.get(`/budgets/${month}/daily-remaining`),
  getStatus: (month) => api.get(`/budgets/${month}/status`),
};

export const alertService = {
  getAll: () => api.get('/alerts'),
  getUnread: () => api.get('/alerts/unread'),
  getUnreadCount: () => api.get('/alerts/unread/count'),
  markAllRead: () => api.post('/alerts/mark-all-read'),
};

export const dashboardService = {
  getData: () => api.get('/dashboard'),
};

export const reportService = {
  getMonthly: (year, month) => api.get(`/reports/monthly/${year}/${month}`),
  getCurrentMonth: () => api.get('/reports/monthly/current'),
  getQuarterly: (year, quarter) => api.get(`/reports/quarterly/${year}/${quarter}`),
  getInsights: () => api.get('/reports/insights'),
};

export default api;
