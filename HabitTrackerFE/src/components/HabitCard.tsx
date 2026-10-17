import { Link } from "react-router-dom";
import type { DashboardHabit, EntryResponse } from "../lib/types";
import { EntryLogger } from "./EntryLogger";
import "./HabitCard.css";

interface HabitCardProps {
  habit: DashboardHabit;
  span?: "wide" | "normal";
  onFieldLogged: (habitFieldSeqId: number, entry: EntryResponse) => void;
}

export function HabitCard({ habit, span = "normal", onFieldLogged }: HabitCardProps) {
  return (
    <article className={`habit-card${span === "wide" ? " habit-card--wide" : ""}`}>
      <div className="habit-card__head">
        <h3 className="habit-card__title">
          <Link to={`/habits/${habit.habitSeqId}`}>{habit.name}</Link>
        </h3>
      </div>
      <div className="habit-card__fields">
        {habit.fields.map((field) => {
          const hasGoal = field.goalValue !== null;
          const numericValue = field.todayValue !== null ? Number(field.todayValue) : null;
          const progress =
            hasGoal && numericValue !== null ? Math.min(1, numericValue / (field.goalValue ?? 1)) : null;

          return (
            <div key={field.habitFieldSeqId} className="habit-field">
              <div className="habit-field__meta">
                <span className="habit-field__name">{field.fieldName}</span>
                {hasGoal && numericValue !== null && (
                  <span className="habit-field__progress-label">
                    {field.todayValue} / {field.goalValue}
                    {field.unit ? ` ${field.unit}` : ""}
                  </span>
                )}
              </div>
              {progress !== null && (
                <div className="habit-field__bar" role="progressbar" aria-valuenow={progress * 100} aria-valuemin={0} aria-valuemax={100}>
                  <span className="habit-field__bar-fill" style={{ width: `${progress * 100}%` }} />
                </div>
              )}
              <EntryLogger
                habitFieldSeqId={field.habitFieldSeqId}
                fieldType={field.fieldType}
                unit={field.unit}
                goalValue={field.goalValue}
                initialValue={field.todayValue}
                initialCompleted={field.completedToday}
                onLogged={(entry) => onFieldLogged(field.habitFieldSeqId, entry)}
              />
            </div>
          );
        })}
      </div>
    </article>
  );
}
