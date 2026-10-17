import type { ConsistencyResponse } from "../lib/types";
import "./ConsistencyCalendar.css";

interface ConsistencyCalendarProps {
  data: ConsistencyResponse;
}

function dateRange(start: string, end: string): string[] {
  const dates: string[] = [];
  const cursor = new Date(`${start}T00:00:00`);
  const last = new Date(`${end}T00:00:00`);
  while (cursor <= last) {
    dates.push(cursor.toISOString().slice(0, 10));
    cursor.setDate(cursor.getDate() + 1);
  }
  return dates;
}

export function ConsistencyCalendar({ data }: ConsistencyCalendarProps) {
  const byDate = new Map(data.entries.map((entry) => [entry.entryDate, entry.completed]));
  const days = dateRange(data.windowStart, data.windowEnd);

  return (
    <div className="consistency">
      <div className="consistency__grid" role="list" aria-label="Last four weeks">
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
