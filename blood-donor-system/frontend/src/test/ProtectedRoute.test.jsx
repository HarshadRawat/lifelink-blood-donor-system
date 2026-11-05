import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import * as AuthContext from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

describe('ProtectedRoute Component', () => {
  it('redirects unauthenticated user to login', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      isAuthenticated: false,
      role: null,
    });

    render(
      <MemoryRouter initialEntries={['/donor/dashboard']}>
        <Routes>
          <Route
            path="/donor/dashboard"
            element={
              <ProtectedRoute allowedRoles={['DONOR']}>
                <div>Donor Protected Content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div>Login Page Redirected</div>} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('Login Page Redirected')).toBeInTheDocument();
    expect(screen.queryByText('Donor Protected Content')).not.toBeInTheDocument();
  });

  it('renders Access Denied when authenticated user lacks required role', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      role: 'HOSPITAL',
    });

    render(
      <MemoryRouter initialEntries={['/donor/dashboard']}>
        <ProtectedRoute allowedRoles={['DONOR']}>
          <div>Donor Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(screen.queryByText('Donor Protected Content')).not.toBeInTheDocument();
  });

  it('renders protected child component when authenticated with allowed role', () => {
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      role: 'DONOR',
    });

    render(
      <MemoryRouter initialEntries={['/donor/dashboard']}>
        <ProtectedRoute allowedRoles={['DONOR']}>
          <div>Donor Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );

    expect(screen.getByText('Donor Protected Content')).toBeInTheDocument();
  });
});

