import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { dashboardService, alertService } from '../services/api';
import { AlertProvider, useAlert } from '../context/AlertContext';
import {
  PieChart, Pie, Cell, BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import {
  TrendingUp, TrendingDown, Wallet, AlertTriangle,
  CheckCircle, Info, ArrowRight, DollarSign, CreditCard
} from 'lucide-react';

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];

const DashboardContent = () => {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { alerts, fetchAlerts, markAllRead } = useAlert();

  useEffect(() => {
    fetchDashboard();
    fetchAlerts();
  }, []);

  const fetchDashboard = async () => {
    try {
      const response = await dashboardService.getData();
      setDashboard(response.data);
    } catch (err) {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const getCategoryChartData = () => {
    if (!dashboard?.categoryBreakdown) return [];
    return Object.entries(dashboard.categoryBreakdown).map(([name, value]) => ({
      name,
      value: parseFloat(value)
    }));
  };

  const getWeeklyTrendData = () => {
    if (!dashboard?.weeklyTrend) return [];
    return Object.entries(dashboard.weeklyTrend).map(([week, amount]) => ({
      week,
      amount: parseFloat(amount)
    }));
  };

  const getSeverityColor = (severity) => {
    switch (severity) {
      case 'warning': return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'critical': return 'text-red-600 bg-red-50 border-red-200';
      case 'success': return 'text-green-600 bg-green-50 border-green-200';
      default: return 'text-blue-600 bg-blue-50 border-blue-200';
    }
  };

  const getSeverityIcon = (severity) => {
    switch (severity) {
      case 'warning': return <AlertTriangle className="h-4 w-4" />;
      case 'critical': return <AlertTriangle className="h-4 w-4" />;
      case 'success': return <CheckCircle className="h-4 w-4" />;
      default: return <Info className="h-4 w-4" />;
    }
  };

  const getBudgetStatusColor = (status) => {
    switch (status) {
      case 'EXCEEDED': return 'text-red-600 bg-red-50';
      case 'WARNING': return 'text-yellow-600 bg-yellow-50';
      case 'GOOD': return 'text-green-600 bg-green-50';
      default: return 'text-gray-600 bg-gray-50';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-600 p-4 rounded-md">
        {error}
      </div>
    );
  }

  const categoryData = getCategoryChartData();
  const weeklyData = getWeeklyTrendData();

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <Link
          to="/add-transaction"
          className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-md hover:bg-primary-700"
        >
          <DollarSign className="h-4 w-4 mr-2" />
          Add Transaction
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Income</p>
              <p className="text-2xl font-bold text-green-600">
                ₹{dashboard?.totalIncome?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-green-100 p-3 rounded-full">
              <TrendingUp className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Expense</p>
              <p className="text-2xl font-bold text-red-600">
                ₹{dashboard?.totalExpense?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-red-100 p-3 rounded-full">
              <TrendingDown className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Savings</p>
              <p className={`text-2xl font-bold ${dashboard?.savings >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                ₹{dashboard?.savings?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-blue-100 p-3 rounded-full">
              <Wallet className="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Remaining Budget</p>
              <p className="text-2xl font-bold text-primary-600">
                ₹{dashboard?.remainingBudget?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className={`p-3 rounded-full ${getBudgetStatusColor(dashboard?.budgetStatus)}`}>
              <CreditCard className="h-6 w-6" />
            </div>
          </div>
          <div className="mt-2">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className={`h-2 rounded-full transition-all ${
                  dashboard?.budgetUsagePercentage > 100 ? 'bg-red-500' :
                  dashboard?.budgetUsagePercentage > 80 ? 'bg-yellow-500' : 'bg-green-500'
                }`}
                style={{ width: `${Math.min(dashboard?.budgetUsagePercentage || 0, 100)}%` }}
              ></div>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              {dashboard?.budgetUsagePercentage?.toFixed(1) || 0}% used
            </p>
          </div>
        </div>
      </div>

      {dashboard?.recentAlerts?.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Recent Alerts</h2>
            <button
              onClick={markAllRead}
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              Mark all as read
            </button>
          </div>
          <div className="space-y-3">
            {alerts.slice(0, 5).map((alert, index) => (
              <div
                key={index}
                className={`flex items-start p-3 rounded-lg border ${getSeverityColor(alert.type?.toLowerCase())}`}
              >
                {getSeverityIcon(alert.type?.toLowerCase())}
                <div className="ml-3 flex-1">
                  <p className="font-medium">{alert.title}</p>
                  <p className="text-sm opacity-80">{alert.message}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {dashboard?.insights?.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Smart Insights</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {dashboard.insights.slice(0, 4).map((insight, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border ${getSeverityColor(insight.severity)}`}
              >
                <div className="flex items-start">
                  {getSeverityIcon(insight.severity)}
                  <div className="ml-3">
                    <p className="font-medium">{insight.title}</p>
                    <p className="text-sm opacity-80 mt-1">{insight.message}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Spending by Category</h2>
          {categoryData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={100}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {categoryData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => `₹${parseFloat(value).toFixed(2)}`} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-64 flex items-center justify-center text-gray-400">
              No expense data available
            </div>
          )}
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Weekly Spending Trend</h2>
          {weeklyData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={weeklyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="week" />
                <YAxis />
                <Tooltip formatter={(value) => `₹${parseFloat(value).toFixed(2)}`} />
                <Line
                  type="monotone"
                  dataKey="amount"
                  stroke="#3B82F6"
                  strokeWidth={2}
                  dot={{ fill: '#3B82F6' }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-64 flex items-center justify-center text-gray-400">
              No weekly data available
            </div>
          )}
        </div>
      </div>

      <div className="flex justify-end">
        <Link
          to="/reports"
          className="inline-flex items-center text-primary-600 hover:text-primary-700 font-medium"
        >
          View Full Reports
          <ArrowRight className="h-4 w-4 ml-1" />
        </Link>
      </div>
    </div>
  );
};

const Dashboard = () => (
  <AlertProvider>
    <DashboardContent />
  </AlertProvider>
);

export default Dashboard;
