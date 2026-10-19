import { useState } from "react";
import { Modal } from "./Modal";
import "./ConfirmDialog.css";

interface ConfirmDialogProps {
  title: string;
  message: string;
  confirmLabel: string;
  onConfirm: () => Promise<void>;
  onCancel: () => void;
}

/** Asks before a destructive action. Mount it only while a confirmation is pending. */
export function ConfirmDialog({ title, message, confirmLabel, onConfirm, onCancel }: ConfirmDialogProps) {
  const [working, setWorking] = useState(false);

  async function handleConfirm() {
    setWorking(true);
    try {
      await onConfirm();
    } finally {
      setWorking(false);
    }
  }

  return (
    <Modal open title={title} onClose={() => !working && onCancel()}>
      <p className="confirm-dialog__message">{message}</p>
      <div className="confirm-dialog__actions">
        <button type="button" className="btn btn--outline" onClick={onCancel} disabled={working} autoFocus>
          Cancel
        </button>
        <button type="button" className="btn btn--danger-solid" onClick={handleConfirm} disabled={working}>
          {working ? <span className="spinner" aria-hidden /> : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
