import { useState, useEffect } from 'react';
import { budgetService } from '../services/api';
import { format } from 'date-fns';
import { Wallet, AlertTriangle, TrendingUp, Calendar, Save } from 'lucide-react';

const Budget = () => {
  const [budget, setBudget] = useState(null);
  const [dailyRemaining, setDailyRemaining] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formData, setFormData] = useState({
    initialBudget: '',
    month: format(new Date(), 'yyyy-MM'),
  });

  useEffect(() => {
    fetchBudget(formData.month);
  }, [formData.month]);

  const fetchBudget = async (month = formData.month) => {
    setError('');
    try {
      const response = await budgetService.getByMonth(month);
      setBudget(response.data);
      setFormData(prev => ({
        ...prev,
        initialBudget: response.data.initialBudget ? response.data.initialBudget.toString() : '',
        month: response.data.month || month,
      }));
      const dailyResp = await budgetService.getDailyRemaining(month);
      setDailyRemaining(dailyResp.data);
    } catch (err) {
      setError('Failed to load budget data');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);

    try {
      const payload = {
        initialBudget: parseFloat(formData.initialBudget),
        month: formData.month,
      };
      await budgetService.create(payload);
      setSuccess('Budget set successfully!');
      fetchBudget(payload.month);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to set budget');
    } finally {
      setSaving(false);
    }
  };

  const getProgressColor = (percentage) => {
    if (percentage >= 100) return 'bg-red-500';
    if (percentage >= 80) return 'bg-yellow-500';
    return 'bg-green-500';
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'EXCEEDED':
      case 'CRITICAL':
        return { bg: 'bg-red-100', text: 'text-red-700', label: 'Budget Exceeded' };
      case 'WARNING':
        return { bg: 'bg-yellow-100', text: 'text-yellow-700', label: 'Near Limit' };
      case 'GOOD':
      case 'NORMAL':
        return { bg: 'bg-green-100', text: 'text-green-700', label: 'On Track' };
      default:
        return { bg: 'bg-gray-100', text: 'text-gray-700', label: 'No Budget' };
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  const statusBadge = getStatusBadge(budget?.status);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Budget Management</h1>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-600 p-4 rounded-md">
          {error}
        </div>
      )}

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-600 p-4 rounded-md">
          {success}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Initial Budget</p>
              <p className="text-2xl font-bold text-gray-900">
                ₹{budget?.initialBudget?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-primary-100 p-3 rounded-full">
              <Wallet className="h-6 w-6 text-primary-600" />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Spent</p>
              <p className="text-2xl font-bold text-red-600">
                ₹{budget?.totalSpent?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-red-100 p-3 rounded-full">
              <TrendingUp className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Remaining</p>
              <p className="text-2xl font-bold text-green-600">
                ₹{budget?.remainingBudget?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div className="bg-green-100 p-3 rounded-full">
              <Calendar className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>
      </div>

      {budget?.isEmergency && (
        <div className="bg-yellow-50 border border-yellow-200 p-4 rounded-xl flex items-start">
          <AlertTriangle className="h-5 w-5 text-yellow-600 mt-0.5 mr-3" />
          <div>
            <p className="font-medium text-yellow-800">Emergency Spending Detected</p>
            <p className="text-sm text-yellow-700 mt-1">
              ₹{budget?.emergencySpent?.toFixed(2)} was spent on emergencies. Your budget has been
              temporarily adjusted to ₹{budget?.adjustedBudget?.toFixed(2)}.
            </p>
          </div>
        </div>
      )}

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">Daily Budget</h2>
          <span className={`px-3 py-1 text-sm font-medium rounded-full ${statusBadge.bg} ${statusBadge.text}`}>
            {statusBadge.label}
          </span>
        </div>

        <div className="mb-4">
          <div className="flex justify-between text-sm text-gray-600 mb-1">
            <span>Budget Used</span>
            <span>{budget?.usagePercentage?.toFixed(1) || 0}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-3">
            <div
              className={`h-3 rounded-full transition-all ${getProgressColor(budget?.usagePercentage || 0)}`}
              style={{ width: `${Math.min(budget?.usagePercentage || 0, 100)}%` }}
            ></div>
          </div>
        </div>

        <div className="p-4 bg-blue-50 rounded-lg">
          <p className="text-sm text-blue-800">
            <span className="font-medium">Daily Budget Remaining:</span> ₹{dailyRemaining.toFixed(2)}
          </p>
          <p className="text-xs text-blue-600 mt-1">
            Based on remaining days in the month
          </p>
        </div>
      </div>

      <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          {budget ? 'Update Budget' : 'Set Budget'}
        </h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Monthly Budget (₹)
              </label>
              <input
                type="number"
                name="initialBudget"
                value={formData.initialBudget}
                onChange={handleChange}
                step="0.01"
                min="0"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="10000"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Month
              </label>
              <input
                type="month"
                name="month"
                value={formData.month}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center px-4 py-2 bg-primary-600 text-white font-medium rounded-md hover:bg-primary-700 disabled:opacity-50"
            >
              <Save className="h-4 w-4 mr-2" />
              {saving ? 'Saving...' : 'Save Budget'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Budget;
