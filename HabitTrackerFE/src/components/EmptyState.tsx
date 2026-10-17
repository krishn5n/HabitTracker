import "./EmptyState.css";

interface EmptyStateProps {
  title: string;
  body: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function EmptyState({ title, body, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <h3 className="empty-state__title">{title}</h3>
      <p className="empty-state__body">{body}</p>
      {actionLabel && onAction && (
        <button type="button" className="btn btn--primary btn--sm" onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
