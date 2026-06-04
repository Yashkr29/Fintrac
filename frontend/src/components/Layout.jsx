import { Outlet, Link, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { AlertProvider, useAlert } from '../context/AlertContext';
import { Home, Receipt, PlusCircle, Wallet, BarChart3, Bell, LogOut } from 'lucide-react';

const Navigation = () => {
  const location = useLocation();
  const { unreadCount, alerts, fetchAlerts, markAllRead } = useAlert();
  const [alertsOpen, setAlertsOpen] = useState(false);

  const navItems = [
    { path: '/', icon: Home, label: 'Dashboard' },
    { path: '/transactions', icon: Receipt, label: 'Transactions' },
    { path: '/add-transaction', icon: PlusCircle, label: 'Add' },
    { path: '/budget', icon: Wallet, label: 'Budget' },
    { path: '/reports', icon: BarChart3, label: 'Reports' },
  ];

  const handleLogout = () => {
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  const handleAlertsToggle = async () => {
    if (!alertsOpen) {
      await fetchAlerts();
    }
    setAlertsOpen((open) => !open);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex">
              <div className="flex-shrink-0 flex items-center">
                <Wallet className="h-8 w-8 text-primary-600" />
                <span className="ml-2 text-xl font-bold text-gray-900">Fintrac</span>
              </div>
              <div className="hidden sm:ml-8 sm:flex sm:space-x-4">
                {navItems.map((item) => (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`inline-flex items-center px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                      location.pathname === item.path
                        ? 'text-primary-600 bg-primary-50'
                        : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    <item.icon className="h-4 w-4 mr-1.5" />
                    {item.label}
                  </Link>
                ))}
              </div>
            </div>
            <div className="flex items-center space-x-3">
              <div className="relative">
              <button
                onClick={handleAlertsToggle}
                className="relative p-2 text-gray-500 hover:text-gray-700"
                aria-label="View budget alerts"
              >
                <Bell className="h-5 w-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-0 right-0 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white transform translate-x-1/4 -translate-y-1/4 bg-red-500 rounded-full">
                    {unreadCount}
                  </span>
                )}
              </button>
              {alertsOpen && (
                <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-white border border-gray-200 rounded-lg shadow-lg z-20">
                  <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
                    <p className="text-sm font-semibold text-gray-900">Alerts</p>
                    {unreadCount > 0 && (
                      <button onClick={markAllRead} className="text-xs font-medium text-primary-600 hover:text-primary-700">
                        Mark all read
                      </button>
                    )}
                  </div>
                  <div className="max-h-80 overflow-y-auto">
                    {alerts.length === 0 ? (
                      <p className="px-4 py-6 text-sm text-gray-500 text-center">No unread alerts</p>
                    ) : (
                      alerts.slice(0, 6).map((alert) => (
                        <div key={alert.id || alert.title} className="px-4 py-3 border-b border-gray-100 last:border-b-0">
                          <p className="text-sm font-medium text-gray-900">{alert.title}</p>
                          <p className="text-xs text-gray-500 mt-1">{alert.message}</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
              </div>
              <button
                onClick={handleLogout}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-500 hover:text-gray-700"
              >
                <LogOut className="h-4 w-4 mr-1.5" />
                Logout
              </button>
            </div>
          </div>
          <div className="sm:hidden grid grid-cols-5 gap-1 pb-3">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`flex flex-col items-center justify-center rounded-md px-2 py-2 text-xs font-medium ${
                  location.pathname === item.path
                    ? 'text-primary-600 bg-primary-50'
                    : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                }`}
              >
                <item.icon className="h-4 w-4 mb-1" />
                {item.label}
              </Link>
            ))}
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  );
};

const LayoutWithAlert = () => (
  <AlertProvider>
    <Navigation />
  </AlertProvider>
);

export default LayoutWithAlert;
