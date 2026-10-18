import type { EntryResponse } from "../lib/types";
import "./MonthCalendarGrid.css";

const WEEKDAYS = ["S", "M", "T", "W", "T", "F", "S"];

interface MonthCalendarGridProps {
  month: Date;
  entries: EntryResponse[];
  onPrevMonth: () => void;
  onNextMonth: () => void;
  disableNext?: boolean;
}

function cellState(entry: EntryResponse | undefined): "achieved" | "partial" | "empty" {
  if (!entry) return "empty";
  return entry.completed ? "achieved" : "partial";
}

export function MonthCalendarGrid({ month, entries, onPrevMonth, onNextMonth, disableNext = false }: MonthCalendarGridProps) {
  const byDate = new Map(entries.map((e) => [e.entryDate, e]));
  const year = month.getFullYear();
  const monthIndex = month.getMonth();
  const firstDay = new Date(year, monthIndex, 1);
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate();
  const leadingBlanks = firstDay.getDay();
  const todayIso = new Date().toISOString().slice(0, 10);
  const label = month.toLocaleDateString(undefined, { month: "long", year: "numeric" });

  const cells = [];
  for (let i = 0; i < leadingBlanks; i++) {
    cells.push(<span key={`blank-${i}`} className="month-grid__cell month-grid__cell--blank" />);
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const iso = `${year}-${String(monthIndex + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    const entry = byDate.get(iso);
    const state = cellState(entry);
    const isToday = iso === todayIso;
    cells.push(
      <span key={iso} className="month-grid__cell" title={entry ? `${iso} — ${entry.entryValue}` : iso}>
        <span className={`month-grid__day month-grid__day--${state}${isToday ? " month-grid__day--today" : ""}`}>
          {day}
        </span>
        {entry && <span className="month-grid__value">{entry.entryValue}</span>}
      </span>,
    );
  }

  return (
    <div className="month-grid">
      <div className="month-grid__nav">
        <button type="button" className="btn btn--ghost btn--sm" onClick={onPrevMonth} aria-label="Previous month">
          ← Prev
        </button>
        <span className="month-grid__label">{label}</span>
        <button
          type="button"
          className="btn btn--ghost btn--sm"
          onClick={onNextMonth}
          disabled={disableNext}
          aria-label="Next month"
        >
          Next →
        </button>
      </div>
      <div className="month-grid__weekdays" aria-hidden>
        {WEEKDAYS.map((w, i) => (
          <span key={i} className="month-grid__weekday">
            {w}
          </span>
        ))}
      </div>
      <div className="month-grid__days" role="list" aria-label={label}>
        {cells}
      </div>
    </div>
  );
}
