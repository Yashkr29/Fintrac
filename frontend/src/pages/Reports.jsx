import { useState, useEffect } from 'react';
import { reportService } from '../services/api';
import {
  PieChart, Pie, Cell, BarChart, Bar, LineChart, Line,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { format } from 'date-fns';
import { FileText, Download, TrendingUp, TrendingDown, DollarSign } from 'lucide-react';

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316'];

const Reports = () => {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedMonth, setSelectedMonth] = useState(format(new Date(), 'yyyy-MM'));

  useEffect(() => {
    fetchReport();
  }, [selectedMonth]);

  const fetchReport = async () => {
    setLoading(true);
    setError('');
    try {
      const [year, month] = selectedMonth.split('-').map(Number);
      const response = await reportService.getMonthly(year, month);
      setReport(response.data);
    } catch (err) {
      setError('Failed to load report');
    } finally {
      setLoading(false);
    }
  };

  const getCategoryChartData = () => {
    if (!report?.categoryWiseBreakdown) return [];
    return Object.entries(report.categoryWiseBreakdown).map(([name, value]) => ({
      name,
      value: parseFloat(value)
    }));
  };

  const getMerchantChartData = () => {
    if (!report?.merchantWiseSpending) return [];
    return Object.entries(report.merchantWiseSpending)
      .sort((a, b) => parseFloat(b[1]) - parseFloat(a[1]))
      .slice(0, 5)
      .map(([name, value]) => ({
        name,
        value: parseFloat(value)
      }));
  };

  const formatCurrency = (amount) => `₹${parseFloat(amount || 0).toFixed(2)}`;

  const getWeeklyChartData = () => {
    if (!report?.weeklySummaries) return [];
    return report.weeklySummaries.map((week) => ({
      week: `Week ${week.weekNumber}`,
      income: parseFloat(week.totalIncome || 0),
      expense: parseFloat(week.totalExpense || 0),
      savings: parseFloat(week.netSavings || 0),
    }));
  };

  const handleDownloadCsv = () => {
    if (!report) return;
    const rows = [
      ['Metric', 'Value'],
      ['Month', report.month],
      ['Total Income', report.totalIncome || 0],
      ['Total Expense', report.totalExpense || 0],
      ['Savings', report.savings || 0],
      ['Budget Difference', report.budgetVsActual || 0],
      ['Savings Rate', `${report.savingsRate || 0}%`],
    ];
    const csv = rows.map((row) => row.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `fintrac-report-${report.month}.csv`;
    link.click();
    URL.revokeObjectURL(url);
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
  const merchantData = getMerchantChartData();
  const weeklyData = getWeeklyChartData();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:justify-between sm:items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Monthly Report</h1>
          <p className="text-sm text-gray-500 mt-1">Category mix, merchants, weekly movement, and budget variance.</p>
        </div>
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
          <input
            type="month"
            value={selectedMonth}
            onChange={(e) => setSelectedMonth(e.target.value)}
            max={format(new Date(), 'yyyy-MM')}
            className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <button
            type="button"
            onClick={handleDownloadCsv}
            className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 text-gray-700 text-sm font-medium rounded-md hover:bg-gray-50"
          >
            <Download className="h-4 w-4 mr-2" />
            Export CSV
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center">
            <div className="bg-green-100 p-3 rounded-full mr-4">
              <TrendingUp className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Total Income</p>
              <p className="text-xl font-bold text-green-600">{formatCurrency(report?.totalIncome)}</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center">
            <div className="bg-red-100 p-3 rounded-full mr-4">
              <TrendingDown className="h-6 w-6 text-red-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Total Expense</p>
              <p className="text-xl font-bold text-red-600">{formatCurrency(report?.totalExpense)}</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center">
            <div className="bg-blue-100 p-3 rounded-full mr-4">
              <DollarSign className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Savings</p>
              <p className={`text-xl font-bold ${report?.savings >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                {formatCurrency(report?.savings)}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center">
            <div className="bg-purple-100 p-3 rounded-full mr-4">
              <FileText className="h-6 w-6 text-purple-600" />
            </div>
            <div>
              <p className="text-sm text-gray-500">Savings Rate</p>
              <p className="text-xl font-bold text-purple-600">{report?.savingsRate?.toFixed(1) || 0}%</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Category Breakdown</h2>
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
              No expense data for this month
            </div>
          )}
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Top Merchants</h2>
          {merchantData.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={merchantData} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" />
                <YAxis type="category" dataKey="name" width={100} />
                <Tooltip formatter={(value) => `₹${parseFloat(value).toFixed(2)}`} />
                <Bar dataKey="value" fill="#3B82F6" />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-64 flex items-center justify-center text-gray-400">
              No merchant data for this month
            </div>
          )}
        </div>
      </div>

      {weeklyData.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Weekly Income vs Expense</h2>
          <ResponsiveContainer width="100%" height={320}>
            <LineChart data={weeklyData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="week" />
              <YAxis />
              <Tooltip formatter={(value) => `₹${parseFloat(value).toFixed(2)}`} />
              <Legend />
              <Line type="monotone" dataKey="income" stroke="#10B981" strokeWidth={2} />
              <Line type="monotone" dataKey="expense" stroke="#EF4444" strokeWidth={2} />
              <Line type="monotone" dataKey="savings" stroke="#3B82F6" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {report?.weeklySummaries?.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Weekly Summaries</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Week</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Income</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Expense</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Net Savings</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {report.weeklySummaries.map((week) => (
                  <tr key={week.weekNumber}>
                    <td className="px-6 py-4 text-sm text-gray-900">{week.weekRange}</td>
                    <td className="px-6 py-4 text-sm text-green-600 font-medium">+{formatCurrency(week.totalIncome)}</td>
                    <td className="px-6 py-4 text-sm text-red-600 font-medium">-{formatCurrency(week.totalExpense)}</td>
                    <td className={`px-6 py-4 text-sm font-medium ${week.netSavings >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatCurrency(week.netSavings)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Budget vs Actual</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <p className="text-sm text-gray-500">Budget</p>
            <p className="text-2xl font-bold text-gray-900">{formatCurrency(report?.budgetVsActual)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Actual Spent</p>
            <p className="text-2xl font-bold text-red-600">{formatCurrency(report?.totalExpense)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">Difference</p>
            <p className={`text-2xl font-bold ${report?.budgetVsActual >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {formatCurrency(Math.abs(report?.budgetVsActual || 0))}
            </p>
          </div>
        </div>
      </div>

      {report?.insights?.length > 0 && (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Insights</h2>
          <div className="space-y-3">
            {report.insights.map((insight, index) => (
              <div
                key={index}
                className={`p-4 rounded-lg border ${
                  insight.severity === 'warning' ? 'bg-yellow-50 border-yellow-200 text-yellow-800' :
                  insight.severity === 'success' ? 'bg-green-50 border-green-200 text-green-800' :
                  'bg-blue-50 border-blue-200 text-blue-800'
                }`}
              >
                <p className="font-medium">{insight.title}</p>
                <p className="text-sm mt-1">{insight.message}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default Reports;
