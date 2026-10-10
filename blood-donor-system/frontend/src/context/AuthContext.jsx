import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const savedUser = sessionStorage.getItem('lifelink_user');
      return savedUser ? JSON.parse(savedUser) : null;
    } catch (e) {
      return null;
    }
  });

  const [token, setToken] = useState(() => {
    return sessionStorage.getItem('lifelink_token') || null;
  });

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (token) {
      sessionStorage.setItem('lifelink_token', token);
    } else {
      sessionStorage.removeItem('lifelink_token');
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      sessionStorage.setItem('lifelink_user', JSON.stringify(user));
    } else {
      sessionStorage.removeItem('lifelink_user');
    }
  }, [user]);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const response = await api.auth.login({ email, password });
      // Synchronously store in sessionStorage immediately
      if (response && response.token) {
        sessionStorage.setItem('lifelink_token', response.token);
      }
      if (response && response.user) {
        sessionStorage.setItem('lifelink_user', JSON.stringify(response.user));
      }
      setToken(response.token);
      setUser(response.user);
      return response.user;
    } finally {
      setLoading(false);
    }
  };

  const register = async (name, email, password, role) => {
    setLoading(true);
    try {
      const response = await api.auth.register({ name, email, password, role });
      return response;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    sessionStorage.removeItem('lifelink_token');
    sessionStorage.removeItem('lifelink_user');
  };

  const value = {
    user,
    token,
    role: user?.role || null,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
