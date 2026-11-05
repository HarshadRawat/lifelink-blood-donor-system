import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import {
  UrgencyBadge,
  RequestStatusBadge,
  MatchStatusBadge,
  AvailabilityBadge,
} from '../components/Badges';

describe('Badges Component Suite', () => {
  describe('UrgencyBadge', () => {
    it('renders CRITICAL urgency badge with correct uppercase styling', () => {
      render(<UrgencyBadge urgency="critical" />);
      expect(screen.getByText('CRITICAL')).toBeInTheDocument();
    });

    it('renders HIGH urgency badge', () => {
      render(<UrgencyBadge urgency="high" />);
      expect(screen.getByText('HIGH')).toBeInTheDocument();
    });

    it('renders MEDIUM urgency badge', () => {
      render(<UrgencyBadge urgency="medium" />);
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
    });

    it('renders LOW urgency badge', () => {
      render(<UrgencyBadge urgency="low" />);
      expect(screen.getByText('LOW')).toBeInTheDocument();
    });
  });

  describe('RequestStatusBadge', () => {
    it('renders ACTIVE status with indicator dot', () => {
      render(<RequestStatusBadge status="ACTIVE" />);
      expect(screen.getByText('ACTIVE')).toBeInTheDocument();
    });

    it('renders FULFILLED status', () => {
      render(<RequestStatusBadge status="FULFILLED" />);
      expect(screen.getByText('FULFILLED')).toBeInTheDocument();
    });

    it('renders CANCELLED status', () => {
      render(<RequestStatusBadge status="CANCELLED" />);
      expect(screen.getByText('CANCELLED')).toBeInTheDocument();
    });
  });

  describe('MatchStatusBadge', () => {
    it('renders ACCEPTED status badge', () => {
      render(<MatchStatusBadge status="ACCEPTED" />);
      expect(screen.getByText('ACCEPTED')).toBeInTheDocument();
    });

    it('renders REJECTED status badge', () => {
      render(<MatchStatusBadge status="REJECTED" />);
      expect(screen.getByText('REJECTED')).toBeInTheDocument();
    });

    it('renders NOTIFIED status badge', () => {
      render(<MatchStatusBadge status="NOTIFIED" />);
      expect(screen.getByText('NOTIFIED')).toBeInTheDocument();
    });

    it('renders MATCHED status badge', () => {
      render(<MatchStatusBadge status="MATCHED" />);
      expect(screen.getByText('MATCHED')).toBeInTheDocument();
    });
  });

  describe('AvailabilityBadge', () => {
    it('renders AVAILABLE TO DONATE when true', () => {
      render(<AvailabilityBadge available={true} />);
      expect(screen.getByText('AVAILABLE TO DONATE')).toBeInTheDocument();
    });

    it('renders CURRENTLY UNAVAILABLE when false', () => {
      render(<AvailabilityBadge available={false} />);
      expect(screen.getByText('CURRENTLY UNAVAILABLE')).toBeInTheDocument();
    });
  });
});
