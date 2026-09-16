import React from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="navbar">
      <div className="navbar-role">{user?.role} PORTAL</div>
      <div className="navbar-user">
        <span>{user?.name}</span>
        <Link className="btn btn-outline btn-sm" to="/change-password">Account Settings</Link>
        <button className="btn btn-outline btn-sm" onClick={handleLogout}>Log out</button>
      </div>
    </div>
  );
}
