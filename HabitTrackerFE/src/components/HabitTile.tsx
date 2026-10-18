import { Link } from "react-router-dom";
import type { DashboardField } from "../lib/types";
import "./HabitTile.css";

export type FieldStatus = "achieved" | "partial" | "empty";

export function fieldStatus(f: DashboardField): FieldStatus {
  if (f.todayValue == null) return "empty";
  if (f.goalValue == null) return "achieved";
  return f.completedToday ? "achieved" : "partial";
}

interface HabitTileProps {
  habitSeqId: number;
  field: DashboardField;
  wide?: boolean;
}

const STATUS_LABEL: Record<FieldStatus, string> = {
  achieved: "✓ Logged",
  partial: "Below goal",
  empty: "Not logged",
};

export function HabitTile({ habitSeqId, field, wide = false }: HabitTileProps) {
  const status = fieldStatus(field);
  const numericValue = field.todayValue !== null ? Number(field.todayValue) : null;
  const hasGoal = field.goalValue !== null;
  const progress = hasGoal && numericValue !== null ? Math.min(1, numericValue / (field.goalValue ?? 1)) : null;

  return (
    <Link
      to={`/habits/${habitSeqId}/fields/${field.habitFieldSeqId}`}
      className={`habit-tile${wide ? " habit-tile--wide" : ""}`}
    >
      <span className="habit-tile__name">{field.fieldName}</span>
      <span className="habit-tile__value">
        {field.todayValue ?? "—"}
        {field.unit && field.todayValue !== null ? <span className="habit-tile__unit">{field.unit}</span> : null}
      </span>
      {progress !== null && (
        <div className="habit-tile__bar">
          <span
            className={`habit-tile__bar-fill habit-tile__bar-fill--${status === "achieved" ? "achieved" : "partial"}`}
            style={{ width: `${progress * 100}%` }}
          />
        </div>
      )}
      <span className={`habit-tile__status habit-tile__status--${status}`}>
        {status === "partial" && field.goalValue !== null
          ? `${Math.max(0, field.goalValue - (numericValue ?? 0))} to go`
          : STATUS_LABEL[status]}
      </span>
    </Link>
  );
}
