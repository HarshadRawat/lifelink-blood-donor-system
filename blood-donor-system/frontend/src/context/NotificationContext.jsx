import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { useAuth } from './AuthContext';
import { api } from '../api/client';

const NotificationContext = createContext(null);

const getWsUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl) {
    return envUrl.replace(/^http/, 'ws') + '/ws';
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
};
const WS_URL = getWsUrl();

export function NotificationProvider({ children }) {
  const { isAuthenticated, token } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  const fetchNotifications = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      setLoading(true);
      const list = await api.notifications.getMyNotifications();
      setNotifications(list || []);
      const countRes = await api.notifications.getUnreadCount();
      setUnreadCount(countRes?.unreadCount || 0);
    } catch (err) {
      console.error('Failed to fetch notifications:', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  const markAsRead = async (notificationId) => {
    try {
      const updated = await api.notifications.markAsRead(notificationId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notificationId ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
      return updated;
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
    }
  };

  const showToast = (title, message, type = 'info') => {
    setToastMessage({ id: Date.now(), title, message, type });
  };

  const clearToast = () => setToastMessage(null);

  // WebSocket / STOMP Connection setup
  useEffect(() => {
    if (!isAuthenticated || !token) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }

    fetchNotifications();

    const stompClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: function (str) {
        // Logging STOMP debug messages silently
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (frame) => {
      stompClient.subscribe('/user/queue/notifications', (message) => {
        try {
          const notification = JSON.parse(message.body);
          setNotifications((prev) => [notification, ...prev]);
          setUnreadCount((prev) => prev + 1);
          showToast(
            notification.title || 'New Notification',
            notification.message || 'You have received an emergency update.',
            'warning'
          );
        } catch (e) {
          console.error('Failed to parse websocket notification payload', e);
        }
      });
    };

    stompClient.onStompError = (frame) => {
      console.warn('STOMP protocol error:', frame.headers['message']);
    };

    stompClient.activate();

    return () => {
      if (stompClient.active) {
        stompClient.deactivate();
      }
    };
  }, [isAuthenticated, token, fetchNotifications]);

  const value = {
    notifications,
    unreadCount,
    loading,
    toastMessage,
    fetchNotifications,
    markAsRead,
    showToast,
    clearToast,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
