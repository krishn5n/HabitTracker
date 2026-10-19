import type { ConsistencyResponse } from "../lib/types";
import "./ConsistencyCalendar.css";

interface ConsistencyCalendarProps {
  data: ConsistencyResponse;
  windowDays?: number;
}

function trailingDays(windowDays: number): string[] {
  const dates: string[] = [];
  const cursor = new Date();
  cursor.setDate(cursor.getDate() - (windowDays - 1));
  for (let i = 0; i < windowDays; i++) {
    dates.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

export function ConsistencyCalendar({ data, windowDays = 40 }: ConsistencyCalendarProps) {
  const byDate = new Map(data.entries.map((entry) => [entry.entryDate, entry.completed]));
  const days = trailingDays(windowDays);

  return (
    <div className="consistency">
      <div className="consistency__grid" role="list" aria-label="Last 40 days">
        {days.map((date) => {
          const completed = byDate.get(date);
          const state = completed === undefined ? "empty" : completed ? "done" : "missed";
          return (
            <span
              key={date}
              role="listitem"
              className={`consistency__cell consistency__cell--${state}`}
              title={`${date} — ${state === "empty" ? "no entry" : state === "done" ? "goal met" : "logged, goal missed"}`}
            />
          );
        })}
      </div>
      <div className="consistency__legend">
        <span>
          <i className="consistency__swatch consistency__swatch--done" /> goal met
        </span>
        <span>
          <i className="consistency__swatch consistency__swatch--missed" /> logged, missed
        </span>
        <span>
          <i className="consistency__swatch consistency__swatch--empty" /> no entry
        </span>
      </div>
    </div>
  );
}
