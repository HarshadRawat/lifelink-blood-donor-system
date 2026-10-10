import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';
import { Navbar } from './components/Navbar';
import { Footer } from './components/Footer';
import { Toast } from './components/Toast';
import { ProtectedRoute } from './components/ProtectedRoute';

import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { BloodGroupsPage } from './pages/BloodGroupsPage';
import { DonationGuidePage } from './pages/DonationGuidePage';
import { AboutPage } from './pages/AboutPage';
import { DonorDashboard } from './pages/DonorDashboard';
import { HospitalDashboard } from './pages/HospitalDashboard';
import { AdminDashboard } from './pages/AdminDashboard';
import { NotFoundPage } from './pages/NotFoundPage';

import { ErrorBoundary } from './components/ErrorBoundary';

import './App.css';

function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <NotificationProvider>
            <div className="flex flex-col min-h-screen font-sans bg-slate-50 text-slate-800">
              <Navbar />
              
              <main className="flex-grow">
              <Routes>
                {/* Public Educational & Auth Routes */}
                <Route path="/" element={<LandingPage />} />
                <Route path="/blood-groups" element={<BloodGroupsPage />} />
                <Route path="/donation-guide" element={<DonationGuidePage />} />
                <Route path="/about" element={<AboutPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* Role-Protected Routes */}
                <Route
                  path="/donor/dashboard"
                  element={
                    <ProtectedRoute allowedRoles={['DONOR']}>
                      <DonorDashboard />
                    </ProtectedRoute>
                  }
                />

                <Route
                  path="/hospital/dashboard"
                  element={
                    <ProtectedRoute allowedRoles={['HOSPITAL']}>
                      <HospitalDashboard />
                    </ProtectedRoute>
                  }
                />

                <Route
                  path="/admin/dashboard"
                  element={
                    <ProtectedRoute allowedRoles={['ADMIN']}>
                      <AdminDashboard />
                    </ProtectedRoute>
                  }
                />

                {/* 404 Route */}
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </main>

            <Footer />
            <Toast />
          </div>
        </NotificationProvider>
      </AuthProvider>
    </BrowserRouter>
  </ErrorBoundary>
  );
}

export default App;
