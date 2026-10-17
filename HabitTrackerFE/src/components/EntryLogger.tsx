import { useState } from "react";
import { api, ApiError } from "../lib/api";
import type { EntryResponse, FieldType } from "../lib/types";
import { useToast } from "../context/ToastContext";
import "./EntryLogger.css";

interface EntryLoggerProps {
  habitFieldSeqId: number;
  fieldType: FieldType;
  unit: string | null;
  goalValue: number | null;
  initialValue: string | null;
  initialCompleted: boolean;
  onLogged: (entry: EntryResponse) => void;
}

export function EntryLogger({
  habitFieldSeqId,
  fieldType,
  unit,
  goalValue,
  initialValue,
  initialCompleted,
  onLogged,
}: EntryLoggerProps) {
  const { push } = useToast();
  const [draft, setDraft] = useState(initialValue ?? "");
  const [completed, setCompleted] = useState(initialCompleted);
  const [status, setStatus] = useState<"idle" | "saving" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function logValue(value: string) {
    setStatus("saving");
    setError(null);
    const prevCompleted = completed;
    const prevDraft = draft;
    setDraft(value);
    try {
      const entry = await api.put<EntryResponse>(`/api/habit-fields/${habitFieldSeqId}/entries`, {
        entryValue: value,
      });
      setCompleted(entry.completed);
      setStatus("idle");
      onLogged(entry);
    } catch (err) {
      setCompleted(prevCompleted);
      setDraft(prevDraft);
      setStatus("error");
      const message = err instanceof ApiError ? err.detail : "Couldn't save that entry.";
      setError(message);
      push({ tone: "error", message, action: { label: "Try again", run: () => logValue(value) } });
    }
  }

  if (fieldType === "BOOLEAN") {
    return (
      <button
        type="button"
        className={`entry-toggle${completed ? " entry-toggle--done" : ""}`}
        role="switch"
        aria-checked={completed}
        disabled={status === "saving"}
        onClick={() => logValue(completed ? "false" : "true")}
      >
        {status === "saving" ? <span className="spinner" aria-hidden /> : completed ? "Done" : "Mark done"}
      </button>
    );
  }

  return (
    <form
      className="entry-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (draft.trim() !== "") logValue(draft.trim());
      }}
    >
      <div className="entry-form__row">
        <input
          className="control entry-form__input"
          aria-invalid={status === "error"}
          type={fieldType === "NUMBER" || fieldType === "DURATION" ? "number" : "text"}
          inputMode={fieldType === "NUMBER" || fieldType === "DURATION" ? "decimal" : undefined}
          step={fieldType === "NUMBER" || fieldType === "DURATION" ? "any" : undefined}
          placeholder={goalValue ? `Goal ${goalValue}${unit ? ` ${unit}` : ""}` : unit ?? "Value"}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          disabled={status === "saving"}
        />
        <button type="submit" className="btn btn--outline btn--sm" disabled={status === "saving" || draft.trim() === ""}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : "Log"}
        </button>
      </div>
      <p className="field__helper field__helper--error" aria-live="polite">
        {error ?? ""}
      </p>
    </form>
  );
}
