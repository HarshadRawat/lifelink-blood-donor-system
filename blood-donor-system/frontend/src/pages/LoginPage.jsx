import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail, HeartHandshake, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { login } = useAuth();
  const { showToast } = useNotifications();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email.trim() || !password.trim()) {
      setErrorMessage('Please fill in both email and password.');
      return;
    }

    setSubmitting(true);
    try {
      const user = await login(email, password);
      showToast('Welcome back', `Logged in as ${user.name} (${user.role})`, 'success');

      // Redirect based on role
      if (user.role === 'DONOR') {
        navigate('/donor/dashboard');
      } else if (user.role === 'HOSPITAL') {
        navigate('/hospital/dashboard');
      } else if (user.role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else {
        navigate('/');
      }
    } catch (err) {
      setErrorMessage(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <Link to="/" className="auth-logo">
            <span className="logo-icon-bg">
              <HeartHandshake size={24} className="text-white" />
            </span>
            <span className="logo-text">
              Life<span className="logo-text-highlight">Link</span>
            </span>
          </Link>
          <h2 className="auth-title">Welcome Back</h2>
          <p className="auth-subtitle">Sign in to access your blood response portal</p>
        </div>

        {errorMessage && (
          <div className="alert alert-error">
            <AlertCircle size={18} className="alert-icon" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              Email Address
            </label>
            <div className="input-icon-wrapper">
              <Mail className="input-icon" size={18} />
              <input
                id="email"
                type="email"
                required
                className="form-input"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

          <div className="form-group">
            <div className="flex justify-between items-center mb-1">
              <label className="form-label mb-0" htmlFor="password">
                Password
              </label>
            </div>
            <div className="input-icon-wrapper">
              <Lock className="input-icon" size={18} />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                required
                className="form-input"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={submitting}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="submit-btn font-medium cursor-pointer"
          >
            {submitting ? (
              <span className="flex items-center justify-center gap-2">
                <span className="spinner-sm" /> Authenticating...
              </span>
            ) : (
              'Sign In'
            )}
          </button>
        </form>

        {/* Quick Demo Credentials Panel for instant evaluation */}
        <div className="demo-credentials-box mt-4 p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs">
          <div className="flex items-center justify-between mb-2">
            <span className="font-semibold text-slate-700 flex items-center gap-1">
              ⚡ Quick Demo Logins
            </span>
            <span className="text-slate-400 text-[10px]">1-click autofill</span>
          </div>
          <div className="grid grid-cols-3 gap-1.5">
            <button
              type="button"
              onClick={() => { setEmail('hospital@lifelink.com'); setPassword('Hospital123!'); }}
              className="p-1.5 bg-white border border-slate-200 hover:border-red-300 hover:bg-red-50 rounded text-center transition cursor-pointer"
              title="Hospital Coordinator in Bengaluru"
            >
              <div className="font-medium text-slate-800">🏥 Hospital</div>
              <div className="text-[10px] text-slate-500">City Care</div>
            </button>
            <button
              type="button"
              onClick={() => { setEmail('alice.donor@lifelink.com'); setPassword('Donor123!'); }}
              className="p-1.5 bg-white border border-slate-200 hover:border-red-300 hover:bg-red-50 rounded text-center transition cursor-pointer"
              title="Eligible A+ Donor near Hospital (0.5 km)"
            >
              <div className="font-medium text-slate-800">🩸 Donor A+</div>
              <div className="text-[10px] text-slate-500">Alice (0.5km)</div>
            </button>
            <button
              type="button"
              onClick={() => { setEmail('admin@lifelink.com'); setPassword('Admin123'); }}
              className="p-1.5 bg-white border border-slate-200 hover:border-red-300 hover:bg-red-50 rounded text-center transition cursor-pointer"
              title="LifeLink System Administrator"
            >
              <div className="font-medium text-slate-800">🛡️ Admin</div>
              <div className="text-[10px] text-slate-500">System</div>
            </button>
          </div>
        </div>

        <div className="auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="auth-link">
              Register here
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
