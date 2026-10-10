import React, { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail, User, HeartHandshake, Building2, AlertCircle, Heart } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';

export function RegisterPage() {
  const location = useLocation();
  const defaultRole = location.state?.defaultRole || 'DONOR';

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState(defaultRole);
  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const { register } = useAuth();
  const { showToast } = useNotifications();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!name.trim() || !email.trim() || !password.trim()) {
      setErrorMessage('All fields are required.');
      return;
    }

    if (password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }

    if (role === 'ADMIN') {
      setErrorMessage('Public registration for ADMIN is not permitted.');
      return;
    }

    setSubmitting(true);
    try {
      await register(name.trim(), email.trim(), password, role);
      showToast('Registration successful', 'Please sign in with your new credentials.', 'success');
      navigate('/login');
    } catch (err) {
      setErrorMessage(err.message || 'Registration failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-container py-10">
      <div className="auth-card max-w-md">
        <div className="auth-header">
          <Link to="/" className="auth-logo">
            <span className="logo-icon-bg">
              <HeartHandshake size={24} className="text-white" />
            </span>
            <span className="logo-text">
              Life<span className="logo-text-highlight">Link</span>
            </span>
          </Link>
          <h2 className="auth-title">Create an Account</h2>
          <p className="auth-subtitle">Join the emergency blood response network</p>
        </div>

        {errorMessage && (
          <div className="alert alert-error">
            <AlertCircle size={18} className="alert-icon" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form">
          {/* Account Role Selector */}
          <div className="form-group">
            <label className="form-label">I want to register as a:</label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                className={`role-select-btn ${role === 'DONOR' ? 'active' : ''}`}
                onClick={() => setRole('DONOR')}
              >
                <Heart size={18} className={role === 'DONOR' ? 'text-red-600' : 'text-slate-400'} />
                <span>Blood Donor</span>
              </button>

              <button
                type="button"
                className={`role-select-btn ${role === 'HOSPITAL' ? 'active' : ''}`}
                onClick={() => setRole('HOSPITAL')}
              >
                <Building2 size={18} className={role === 'HOSPITAL' ? 'text-red-600' : 'text-slate-400'} />
                <span>Hospital</span>
              </button>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="name">
              {role === 'HOSPITAL' ? 'Hospital / Institution Name' : 'Full Name'}
            </label>
            <div className="input-icon-wrapper">
              <User className="input-icon" size={18} />
              <input
                id="name"
                type="text"
                required
                className="form-input"
                placeholder={role === 'HOSPITAL' ? 'St. Jude General Hospital' : 'John Doe'}
                value={name}
                onChange={(e) => setName(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

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
                placeholder="contact@institution.org"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={submitting}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">
              Password
            </label>
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
                <span className="spinner-sm" /> Creating Account...
              </span>
            ) : (
              `Register as ${role === 'HOSPITAL' ? 'Hospital' : 'Donor'}`
            )}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="auth-link">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
