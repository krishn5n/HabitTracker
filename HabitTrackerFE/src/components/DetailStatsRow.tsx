import "./SummaryRow.css";

interface DetailStatsRowProps {
  streak: number;
  rateLabel: string;
  goalLabel: string;
}

export function DetailStatsRow({ streak, rateLabel, goalLabel }: DetailStatsRowProps) {
  return (
    <div className="summary-row">
      <div className="summary-row__item">
        <span className="summary-row__value summary-row__value--achieved">{streak}</span>
        <span className="summary-row__label">Day streak</span>
      </div>
      <div className="summary-row__item">
        <span className="summary-row__value">{rateLabel}</span>
        <span className="summary-row__label">Last Week</span>
      </div>
      <div className="summary-row__item">
        <span className="summary-row__value">{goalLabel}</span>
        <span className="summary-row__label">Goal</span>
      </div>
    </div>
  );
}
