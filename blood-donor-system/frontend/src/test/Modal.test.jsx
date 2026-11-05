import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Modal } from '../components/Modal';

describe('Modal Component', () => {
  it('does not render anything when isOpen is false', () => {
    const { container } = render(
      <Modal isOpen={false} onClose={() => {}} title="Test Modal">
        <p>Modal content</p>
      </Modal>
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders modal content and title when isOpen is true', () => {
    render(
      <Modal isOpen={true} onClose={() => {}} title="Emergency Notice">
        <p>Confirm donor contact share</p>
      </Modal>
    );
    expect(screen.getByText('Emergency Notice')).toBeInTheDocument();
    expect(screen.getByText('Confirm donor contact share')).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    const handleClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={handleClose} title="Emergency Notice">
        <p>Modal content</p>
      </Modal>
    );

    const closeButton = screen.getByLabelText('Close dialog');
    fireEvent.click(closeButton);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when Escape key is pressed', () => {
    const handleClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={handleClose} title="Emergency Notice">
        <p>Modal content</p>
      </Modal>
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});

