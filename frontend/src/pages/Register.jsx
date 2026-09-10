import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { academicSettingsApi } from '../services/api';

const emptyForm = {
  name: '', email: '', password: '', role: 'STUDENT',
  rollNumber: '', department: '', degree: '', graduationYear: 2027
};

export default function Register() {
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(false);
  const [pendingMessage, setPendingMessage] = useState(null);
  const [options, setOptions] = useState({ departments: [], degrees: [] });
  const { register } = useAuth();
  const { errorFromException } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    academicSettingsApi.get().then(({ data }) => setOptions(data)).catch(() => {});
  }, []);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await register(form);
      navigate('/');
    } catch (err) {
      // A lecturer registration returns 403 with a "pending approval" message —
      // show that as an informational screen rather than a generic error toast.
      const message = err?.response?.data?.message;
      if (form.role === 'INTERVIEWER' && err?.response?.status === 403 && message) {
        setPendingMessage(message);
      } else {
        errorFromException(err, 'Could not create your account.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (pendingMessage) {
    return (
      <div className="auth-page">
        <div className="auth-card" style={{ width: 440 }}>
          <h1>Registration submitted</h1>
          <p className="muted" style={{ marginTop: 12 }}>{pendingMessage}</p>
          <Link className="btn btn-primary" style={{ marginTop: 20, width: '100%', justifyContent: 'center' }} to="/login">
            Back to sign in
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card" style={{ width: 440 }}>
        <h1>Create your account</h1>
        <p className="muted" style={{ marginBottom: 20 }}>
          {form.role === 'STUDENT'
            ? 'Register as a student to apply for recruitment drives'
            : 'Register as a lecturer / interviewer — an admin must approve your account before you can sign in'}
        </p>

        <div className="form-group">
          <label>I am a</label>
          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button"
              className={form.role === 'STUDENT' ? 'btn btn-primary' : 'btn btn-outline'}
              style={{ flex: 1, justifyContent: 'center' }}
              onClick={() => setForm({ ...form, role: 'STUDENT' })}>
              Student
            </button>
            <button type="button"
              className={form.role === 'INTERVIEWER' ? 'btn btn-primary' : 'btn btn-outline'}
              style={{ flex: 1, justifyContent: 'center' }}
              onClick={() => setForm({ ...form, role: 'INTERVIEWER' })}>
              Lecturer / Interviewer
            </button>
          </div>
        </div>

        <form onSubmit={submit}>
          <div className="form-group">
            <label>Full name</label>
            <input value={form.name} onChange={update('name')} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={form.email} onChange={update('email')} required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={form.password} onChange={update('password')} required />
          </div>

          {form.role === 'STUDENT' ? (
            <>
              <div className="form-row">
                <div className="form-group">
                  <label>Roll number</label>
                  <input value={form.rollNumber} onChange={update('rollNumber')} required />
                </div>
                <div className="form-group">
                  <label>Degree</label>
                  <select value={form.degree} onChange={update('degree')} required>
                    <option value="" disabled>Select…</option>
                    {options.degrees.map((d) => <option key={d} value={d}>{d}</option>)}
                  </select>
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Department</label>
                  <select value={form.department} onChange={update('department')} required>
                    <option value="" disabled>Select…</option>
                    {options.departments.map((d) => <option key={d} value={d}>{d}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label>Graduation year</label>
                  <input type="number" value={form.graduationYear} onChange={update('graduationYear')} required />
                </div>
              </div>
            </>
          ) : (
            <div className="form-group">
              <label>Department</label>
              <select value={form.department} onChange={update('department')} required>
                <option value="" disabled>Select…</option>
                {options.departments.map((d) => <option key={d} value={d}>{d}</option>)}
              </select>
            </div>
          )}

          <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>
        <p className="muted" style={{ marginTop: 18 }}>
          Already have an account? <Link className="link-amber" to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  );
}
