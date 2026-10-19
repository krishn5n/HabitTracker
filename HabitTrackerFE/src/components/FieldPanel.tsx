import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError } from "../lib/api";
import type { ConsistencyResponse, EntryResponse, HabitFieldResponse } from "../lib/types";
import { EntryLogger } from "./EntryLogger";
import "./FieldPanel.css";

interface FieldPanelProps {
  field: HabitFieldResponse;
  onEdit: () => void;
  onDelete: () => void;
}

const todayIso = () => new Date().toISOString().slice(0, 10);

const MS_PER_DAY = 86_400_000;

function windowDays(windowStart: string, windowEnd: string): number {
  const span = Date.parse(windowEnd) - Date.parse(windowStart);
  return Math.max(1, Math.round(span / MS_PER_DAY) + 1);
}

export function FieldPanel({ field, onEdit, onDelete }: FieldPanelProps) {
  const [latest, setLatest] = useState<EntryResponse | null>(null);
  const [consistency, setConsistency] = useState<ConsistencyResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLatest(null);
    setConsistency(null);
    setLoading(true);
    const latestPromise = api
      .get<EntryResponse>(`/api/habit-fields/${field.habitFieldSeqId}/entries/latest`)
      .then((entry) => active && setLatest(entry))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 404) return;
        if (active) setError(err instanceof ApiError ? err.detail : "Couldn't load the latest entry.");
      });
    const consistencyPromise = api
      .get<ConsistencyResponse>(`/api/habit-fields/${field.habitFieldSeqId}/consistency`)
      .then((res) => active && setConsistency(res))
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.detail : "Couldn't load these metrics.");
      });
    Promise.allSettled([latestPromise, consistencyPromise]).then(() => {
      if (active) setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [field.habitFieldSeqId]);

  const today = todayIso();
  const loggedToday = latest?.entryDate === today;
  const logged = consistency?.entries.length ?? 0;
  const trackedDays = consistency ? windowDays(consistency.windowStart, consistency.windowEnd) : 0;
  const metRate = consistency
    ? Math.round((consistency.entries.filter((e) => e.completed).length / trackedDays) * 100)
    : null;
  const goalLabel = field.goalValue !== null ? `${field.goalValue}${field.unit ? ` ${field.unit}` : ""}` : "Any";

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
      <Link
        to={`/habits/${field.habitSeqId}/fields/${field.habitFieldSeqId}`}
        className="field-panel__link"
        aria-label={`Open ${field.fieldName}`}
      />

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

      <div className="field-panel__logger">
        <EntryLogger
          habitFieldSeqId={field.habitFieldSeqId}
          fieldType={field.fieldType}
          unit={field.unit}
          goalValue={field.goalValue}
          initialValue={loggedToday ? latest!.entryValue : null}
          initialCompleted={loggedToday ? latest!.completed : false}
          onLogged={handleLogged}
        />
      </div>

      {loading ? (
        <p className="field__helper">Loading…</p>
      ) : (
        <>
          {latest && !loggedToday && (
            <p className="field-panel__hint">
              Last logged {latest.entryValue}
              {field.unit ? ` ${field.unit}` : ""} on {latest.entryDate}.
            </p>
          )}

          <dl className="field-panel__stats">
            <div className="field-panel__stat">
              <dt>Goal met</dt>
              <dd>{metRate !== null ? `${metRate}%` : "—"}</dd>
            </div>
            <div className="field-panel__stat">
              <dt>Days logged</dt>
              <dd>
                {logged} / {trackedDays}
              </dd>
            </div>
            <div className="field-panel__stat">
              <dt>Goal</dt>
              <dd>{goalLabel}</dd>
            </div>
          </dl>
        </>
      )}

      {error && <p className="field__helper field__helper--error">{error}</p>}
    </div>
  );
}
