import "./SummaryRow.css";

interface SummaryRowProps {
  achieved: number;
  partial: number;
  empty: number;
}

export function SummaryRow({ achieved, partial, empty }: SummaryRowProps) {
  return (
    <div className="summary-row">
      <div className="summary-row__item">
        <span className="summary-row__value summary-row__value--achieved">{achieved}</span>
        <span className="summary-row__label">Achieved</span>
      </div>
      <div className="summary-row__item">
        <span className="summary-row__value summary-row__value--partial">{partial}</span>
        <span className="summary-row__label">Partial</span>
      </div>
      <div className="summary-row__item">
        <span className="summary-row__value summary-row__value--empty">{empty}</span>
        <span className="summary-row__label">Not logged</span>
      </div>
    </div>
  );
}
