import React, { useState } from 'react';
import { changePassword, updateEmail } from '../services/api';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';

export default function ChangePassword() {
  const { user, logout } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const [newEmail, setNewEmail] = useState(user?.email || '');
  const [emailPassword, setEmailPassword] = useState('');
  const [emailLoading, setEmailLoading] = useState(false);

  const { push, errorFromException } = useToast();

  const submitPassword = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      push("New passwords don't match.", 'error');
      return;
    }
    if (newPassword.length < 6) {
      push('New password must be at least 6 characters.', 'error');
      return;
    }
    setLoading(true);
    try {
      await changePassword(currentPassword, newPassword);
      push('Password changed successfully.', 'success');
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('');
    } catch (err) {
      errorFromException(err, 'Could not change password.');
    } finally {
      setLoading(false);
    }
  };

  const submitEmail = async (e) => {
    e.preventDefault();
    setEmailLoading(true);
    try {
      await updateEmail(newEmail, emailPassword);
      push('Email updated. Please log in again with your new email.', 'success');
      setEmailPassword('');
      setTimeout(() => logout(), 1500);
    } catch (err) {
      errorFromException(err, 'Could not update email.');
    } finally {
      setEmailLoading(false);
    }
  };

  return (
    <div>
      <div className="section-title">
        <h1>Account Settings</h1>
      </div>

      <div className="card-grid" style={{ gridTemplateColumns: '1fr 1fr' }}>
        <div className="card">
          <h3 style={{ marginBottom: 14 }}>Change Password</h3>
          <form onSubmit={submitPassword}>
            <div className="form-group">
              <label>Current password</label>
              <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>New password</label>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={6} />
            </div>
            <div className="form-group">
              <label>Confirm new password</label>
              <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={6} />
            </div>
            <button className="btn btn-primary" disabled={loading}>{loading ? 'Saving…' : 'Change Password'}</button>
          </form>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 14 }}>Update Email</h3>
          <p className="muted" style={{ marginBottom: 14 }}>Current: {user?.email}. You'll need to log in again after this.</p>
          <form onSubmit={submitEmail}>
            <div className="form-group">
              <label>New email</label>
              <input type="email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Confirm with your password</label>
              <input type="password" value={emailPassword} onChange={(e) => setEmailPassword(e.target.value)} required />
            </div>
            <button className="btn btn-primary" disabled={emailLoading}>{emailLoading ? 'Saving…' : 'Update Email'}</button>
          </form>
        </div>
      </div>
    </div>
  );
}
