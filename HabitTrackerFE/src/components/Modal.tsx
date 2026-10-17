import { useEffect, useRef, type ReactNode } from "react";
import "./Modal.css";

interface ModalProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export function Modal({ open, title, onClose, children }: ModalProps) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      className="modal"
      onClose={onClose}
      onCancel={onClose}
      aria-labelledby="modal-title"
    >
      <div className="modal__head">
        <h2 id="modal-title" className="modal__title">
          {title}
        </h2>
        <button type="button" className="btn btn--ghost btn--sm" onClick={onClose} aria-label="Close">
          Close
        </button>
      </div>
      <div className="modal__body">{children}</div>
    </dialog>
  );
}
