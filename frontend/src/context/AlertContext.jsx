import { createContext, useContext, useState, useEffect } from 'react';
import { alertService } from '../services/api';

const AlertContext = createContext();

export const useAlert = () => useContext(AlertContext);

export const AlertProvider = ({ children }) => {
  const [unreadCount, setUnreadCount] = useState(0);
  const [alerts, setAlerts] = useState([]);

  const fetchUnreadCount = async () => {
    try {
      const response = await alertService.getUnreadCount();
      setUnreadCount(response.data.count);
    } catch (error) {
      console.error('Error fetching unread count:', error);
    }
  };

  const fetchAlerts = async () => {
    try {
      const response = await alertService.getUnread();
      setAlerts(response.data);
    } catch (error) {
      console.error('Error fetching alerts:', error);
    }
  };

  const markAllRead = async () => {
    try {
      await alertService.markAllRead();
      setUnreadCount(0);
      setAlerts([]);
    } catch (error) {
      console.error('Error marking alerts as read:', error);
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <AlertContext.Provider value={{ unreadCount, alerts, fetchAlerts, markAllRead, fetchUnreadCount }}>
      {children}
    </AlertContext.Provider>
  );
};
