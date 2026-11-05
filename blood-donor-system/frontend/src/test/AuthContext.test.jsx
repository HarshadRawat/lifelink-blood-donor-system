import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { api } from '../api/client';

vi.mock('../api/client', () => ({
  api: {
    auth: {
      login: vi.fn(),
      register: vi.fn(),
    },
  },
}));

// Helper test component
function TestConsumer() {
  const { user, role, isAuthenticated, login, logout } = useAuth();
  return (
    <div>
      <div data-testid="auth-status">{isAuthenticated ? 'authenticated' : 'unauthenticated'}</div>
      <div data-testid="user-role">{role || 'none'}</div>
      <div data-testid="user-name">{user?.name || 'none'}</div>
      <button
        onClick={() => login('donor@lifelink.com', 'Password123')}
        data-testid="login-btn"
      >
        Login
      </button>
      <button onClick={logout} data-testid="logout-btn">
        Logout
      </button>
    </div>
  );
}

describe('AuthContext Suite', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('provides default unauthenticated state when sessionStorage is empty', () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status').textContent).toBe('unauthenticated');
    expect(screen.getByTestId('user-role').textContent).toBe('none');
    expect(screen.getByTestId('user-name').textContent).toBe('none');
  });

  it('restores authenticated user session from sessionStorage on mount', () => {
    sessionStorage.setItem('lifelink_token', 'mock-jwt-token-123');
    sessionStorage.setItem(
      'lifelink_user',
      JSON.stringify({ id: 5, name: 'Dr. Jane Smith', role: 'HOSPITAL' })
    );

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');
    expect(screen.getByTestId('user-role').textContent).toBe('HOSPITAL');
    expect(screen.getByTestId('user-name').textContent).toBe('Dr. Jane Smith');
  });

  it('updates state and persists token upon successful login', async () => {
    api.auth.login.mockResolvedValueOnce({
      token: 'jwt-returned-from-api',
      user: { id: 10, name: 'Alice Donor', role: 'DONOR' },
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    const loginButton = screen.getByTestId('login-btn');
    await act(async () => {
      loginButton.click();
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');
    expect(screen.getByTestId('user-role').textContent).toBe('DONOR');
    expect(screen.getByTestId('user-name').textContent).toBe('Alice Donor');
    expect(sessionStorage.getItem('lifelink_token')).toBe('jwt-returned-from-api');
  });

  it('clears state and sessionStorage upon logout', () => {
    sessionStorage.setItem('lifelink_token', 'mock-token');
    sessionStorage.setItem(
      'lifelink_user',
      JSON.stringify({ id: 1, name: 'Admin', role: 'ADMIN' })
    );

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    expect(screen.getByTestId('auth-status').textContent).toBe('authenticated');

    const logoutButton = screen.getByTestId('logout-btn');
    act(() => {
      logoutButton.click();
    });

    expect(screen.getByTestId('auth-status').textContent).toBe('unauthenticated');
    expect(screen.getByTestId('user-role').textContent).toBe('none');
    expect(sessionStorage.getItem('lifelink_token')).toBeNull();
    expect(sessionStorage.getItem('lifelink_user')).toBeNull();
  });

  it('throws an error when useAuth is called outside AuthProvider', () => {
    // Suppress console.error for expected test throw
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<TestConsumer />)).toThrow(
      'useAuth must be used within an AuthProvider'
    );

    consoleError.mockRestore();
  });
});

