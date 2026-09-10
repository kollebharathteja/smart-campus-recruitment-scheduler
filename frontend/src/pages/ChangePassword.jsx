import React, { useState } from 'react';
import { changePassword } from '../services/api';
import { useToast } from '../context/ToastContext.jsx';

export default function ChangePassword() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { push, errorFromException } = useToast();

  const submit = async (e) => {
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

  return (
    <div>
      <div className="section-title">
        <h1>Change Password</h1>
      </div>
      <div className="card" style={{ maxWidth: 420 }}>
        <form onSubmit={submit}>
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
    </div>
  );
}
