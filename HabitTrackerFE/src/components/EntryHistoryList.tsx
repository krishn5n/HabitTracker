import type { UIEvent } from "react";
import type { EntryResponse } from "../lib/types";
import "./EntryHistoryList.css";

interface EntryHistoryListProps {
  entries: EntryResponse[];
  unit: string | null;
  goalValue: number | null;
  onScrollEnd: () => void;
}

function subLabel(entry: EntryResponse, goalValue: number | null): string {
  if (goalValue === null) return "Logged";
  return entry.completed ? "Goal achieved" : `Below goal of ${goalValue}`;
}

export function EntryHistoryList({ entries, unit, goalValue, onScrollEnd }: EntryHistoryListProps) {
  function handleScroll(event: UIEvent<HTMLDivElement>) {
    const el = event.currentTarget;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 120) onScrollEnd();
  }

  return (
    <div className="entry-history" onScroll={handleScroll}>
      {entries.map((entry) => (
        <div key={entry.entrySeqId} className="entry-history__row">
          <div className="entry-history__meta">
            <span className="entry-history__date">{entry.entryDate}</span>
            <span className="entry-history__sub">{subLabel(entry, goalValue)}</span>
          </div>
          <div className="entry-history__trailing">
            <span className="entry-history__value">
              {entry.entryValue}
              {unit ? ` ${unit}` : ""}
            </span>
            <span className={`entry-history__dot entry-history__dot--${entry.completed ? "achieved" : "partial"}`} />
          </div>
        </div>
      ))}
    </div>
  );
}
