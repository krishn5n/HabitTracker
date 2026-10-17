import { useEffect, useState } from "react";
import { api, ApiError } from "../lib/api";
import type { ConsistencyResponse, EntryResponse, HabitFieldResponse } from "../lib/types";
import { EntryLogger } from "./EntryLogger";
import { ConsistencyCalendar } from "./ConsistencyCalendar";
import "./FieldPanel.css";

interface FieldPanelProps {
  field: HabitFieldResponse;
  onEdit: () => void;
  onDelete: () => void;
}

const todayIso = () => new Date().toISOString().slice(0, 10);

export function FieldPanel({ field, onEdit, onDelete }: FieldPanelProps) {
  const [latest, setLatest] = useState<EntryResponse | null>(null);
  const [consistency, setConsistency] = useState<ConsistencyResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLatest(null);
    setConsistency(null);
    api
      .get<EntryResponse>(`/api/habit-fields/${field.habitFieldSeqId}/entries/latest`)
      .then((entry) => active && setLatest(entry))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) return;
        if (active) setError(err instanceof ApiError ? err.detail : "Couldn't load the latest entry.");
      });
    api
      .get<ConsistencyResponse>(`/api/habit-fields/${field.habitFieldSeqId}/consistency`)
      .then((res) => active && setConsistency(res))
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.detail : "Couldn't load the calendar.");
      });
    return () => {
      active = false;
    };
  }, [field.habitFieldSeqId]);

  const today = todayIso();
  const loggedToday = latest?.entryDate === today;

  function handleLogged(entry: EntryResponse) {
    setLatest(entry);
    setConsistency((current) => {
      if (!current) return current;
      const withoutToday = current.entries.filter((e) => e.entryDate !== entry.entryDate);
      return { ...current, entries: [...withoutToday, { entryDate: entry.entryDate, completed: entry.completed }] };
    });
  }

  return (
    <div className="field-panel">
      <div className="field-panel__head">
        <div>
          <h3 className="field-panel__name">{field.fieldName}</h3>
          <p className="field-panel__meta">
            {field.fieldType}
            {field.unit ? ` · ${field.unit}` : ""}
            {field.goalValue !== null ? ` · goal ${field.goalValue}` : ""}
          </p>
        </div>
        <div className="field-panel__actions">
          <button type="button" className="btn btn--ghost btn--sm" onClick={onEdit}>
            Edit
          </button>
          <button type="button" className="btn btn--danger btn--sm" onClick={onDelete}>
            Delete
          </button>
        </div>
      </div>

      <EntryLogger
        habitFieldSeqId={field.habitFieldSeqId}
        fieldType={field.fieldType}
        unit={field.unit}
        goalValue={field.goalValue}
        initialValue={loggedToday ? latest!.entryValue : null}
        initialCompleted={loggedToday ? latest!.completed : false}
        onLogged={handleLogged}
      />

      {latest && !loggedToday && (
        <p className="field-panel__hint">
          Last logged {latest.entryValue}
          {field.unit ? ` ${field.unit}` : ""} on {latest.entryDate}.
        </p>
      )}

      {consistency && <ConsistencyCalendar data={consistency} />}

      {error && <p className="field__helper field__helper--error">{error}</p>}
    </div>
  );
}
