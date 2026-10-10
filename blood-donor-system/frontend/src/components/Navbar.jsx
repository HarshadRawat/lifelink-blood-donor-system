import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { HeartHandshake, Bell, LogOut, User, Building2, Shield, Menu, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotifications } from '../context/NotificationContext';
import { NotificationDrawer } from './NotificationDrawer';

export function Navbar() {
  const { user, isAuthenticated, logout, role } = useAuth();
  const { unreadCount } = useNotifications();
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getDashboardPath = () => {
    if (role === 'DONOR') return '/donor/dashboard';
    if (role === 'HOSPITAL') return '/hospital/dashboard';
    if (role === 'ADMIN') return '/admin/dashboard';
    return '/';
  };

  const isLanding = location.pathname === '/';

  return (
    <>
      <nav className="navbar">
        <div className="navbar-container">
          <Link to="/" className="logo">
            <span className="logo-icon-bg">
              <HeartHandshake className="logo-icon" size={22} />
            </span>
            <span className="logo-text">
              Life<span className="logo-text-highlight">Link</span>
            </span>
          </Link>

          {/* Desktop Nav Links */}
          <div className="nav-links">
            <Link to="/" className={isLanding ? 'active' : ''}>
              Home
            </Link>
            <a href="/#how-it-works">How It Works</a>
            <Link to="/blood-groups" className={location.pathname === '/blood-groups' ? 'active' : ''}>
              Blood Groups
            </Link>
            <Link to="/donation-guide" className={location.pathname === '/donation-guide' ? 'active' : ''}>
              Donation Guide
            </Link>
            <Link to="/about" className={location.pathname === '/about' ? 'active' : ''}>
              About LifeLink
            </Link>

            {isAuthenticated && (
              <Link to={getDashboardPath()} className="nav-dashboard-link">
                {role === 'DONOR' && <User size={16} />}
                {role === 'HOSPITAL' && <Building2 size={16} />}
                {role === 'ADMIN' && <Shield size={16} />}
                Dashboard
              </Link>
            )}
          </div>

          {/* Nav Right Actions */}
          <div className="nav-actions">
            {isAuthenticated ? (
              <>
                <button
                  onClick={() => setIsDrawerOpen(true)}
                  className="notification-bell-btn"
                  title="Notifications"
                  aria-label="Open notifications"
                >
                  <Bell size={20} />
                  {unreadCount > 0 && (
                    <span className="bell-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                  )}
                </button>

                <div className="user-profile-badge">
                  <div className="avatar-circle">
                    {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                  </div>
                  <div className="user-info-text">
                    <span className="user-name">{user?.name}</span>
                    <span className="user-role-pill">{role}</span>
                  </div>
                </div>

                <button onClick={handleLogout} className="logout-btn" title="Logout">
                  <LogOut size={18} />
                  <span className="hidden sm:inline">Logout</span>
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="login-btn">
                  Login
                </Link>
                <Link to="/register" state={{ defaultRole: 'DONOR' }} className="register-btn">
                  Become a Donor
                </Link>
              </>
            )}

            <button
              className="mobile-menu-toggle"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Toggle navigation menu"
            >
              {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>

        {/* Mobile menu dropdown */}
        {mobileMenuOpen && (
          <div className="mobile-menu font-sans">
            <Link to="/" onClick={() => setMobileMenuOpen(false)}>
              Home
            </Link>
            <a href="/#how-it-works" onClick={() => setMobileMenuOpen(false)}>
              How It Works
            </a>
            <Link to="/blood-groups" onClick={() => setMobileMenuOpen(false)}>
              Blood Groups
            </Link>
            <Link to="/donation-guide" onClick={() => setMobileMenuOpen(false)}>
              Donation Guide
            </Link>
            <Link to="/about" onClick={() => setMobileMenuOpen(false)}>
              About LifeLink
            </Link>
            {isAuthenticated ? (
              <>
                <Link to={getDashboardPath()} onClick={() => setMobileMenuOpen(false)}>
                  My Dashboard ({role})
                </Link>
                <button onClick={handleLogout} className="text-red-600 w-full text-left font-medium">
                  Logout
                </button>
              </>
            ) : (
              <div className="flex flex-col gap-2 pt-2 border-t">
                <Link to="/login" className="login-btn text-center" onClick={() => setMobileMenuOpen(false)}>
                  Login
                </Link>
                <Link to="/register" state={{ defaultRole: 'DONOR' }} className="register-btn text-center" onClick={() => setMobileMenuOpen(false)}>
                  Become a Donor
                </Link>
              </div>
            )}
          </div>
        )}
      </nav>

      <NotificationDrawer isOpen={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} />
    </>
  );
}
