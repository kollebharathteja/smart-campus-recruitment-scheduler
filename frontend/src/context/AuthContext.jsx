import React, { createContext, useContext, useState } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('scr_user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (email, password) => {
    const { data } = await authApi.login({ email, password });
    persist(data);
    return data;
  };

  const register = async (payload) => {
    const { data } = await authApi.register(payload);
    persist(data);
    return data;
  };

  const persist = (data) => {
    const userData = { id: data.userId, name: data.name, email: data.email, role: data.role };
    localStorage.setItem('scr_token', data.token);
    localStorage.setItem('scr_user', JSON.stringify(userData));
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('scr_token');
    localStorage.removeItem('scr_user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
