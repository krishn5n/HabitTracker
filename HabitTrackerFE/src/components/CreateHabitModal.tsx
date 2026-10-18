import { useState, type FormEvent } from "react";
import { Modal } from "./Modal";
import { api, ApiError } from "../lib/api";
import type { FieldType, HabitFieldResponse, HabitResponse } from "../lib/types";
import "./CreateHabitModal.css";

interface CreateHabitModalProps {
  open: boolean;
  userSeqId: number;
  onClose: () => void;
  onCreated: (habit: HabitResponse, field: HabitFieldResponse) => void;
}

const FIELD_TYPES: { value: FieldType; label: string; hint: string }[] = [
  { value: "NUMBER", label: "Number", hint: "Reps, pages, glasses of water…" },
  { value: "DURATION", label: "Duration", hint: "Minutes spent on the habit" },
  { value: "BOOLEAN", label: "Yes / no", hint: "Did you do it today?" },
  { value: "TEXT", label: "Note", hint: "A short free-form entry" },
];

export function CreateHabitModal({ open, userSeqId, onClose, onCreated }: CreateHabitModalProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [fieldType, setFieldType] = useState<FieldType>("NUMBER");
  const [unit, setUnit] = useState("");
  const [goalValue, setGoalValue] = useState("");
  const [status, setStatus] = useState<"idle" | "saving" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  const goalAllowed = fieldType === "NUMBER" || fieldType === "DURATION";

  function reset() {
    setName("");
    setDescription("");
    setFieldType("NUMBER");
    setUnit("");
    setGoalValue("");
    setStatus("idle");
    setError(null);
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (name.trim() === "") return;
    setStatus("saving");
    setError(null);
    try {
      const habit = await api.post<HabitResponse>("/api/habits", {
        userSeqId,
        name: name.trim(),
        description: description.trim() === "" ? undefined : description.trim(),
      });
      const field = await api.post<HabitFieldResponse>(`/api/habits/${habit.habitSeqId}/fields`, {
        fieldType,
        unit: unit.trim() === "" ? undefined : unit.trim(),
        goalValue: goalAllowed && goalValue.trim() !== "" ? Number(goalValue) : undefined,
      });
      onCreated(habit, field);
      reset();
      onClose();
    } catch (err) {
      setStatus("error");
      setError(err instanceof ApiError ? err.detail : "Couldn't create that habit. Try again.");
    }
  }

  return (
    <Modal
      open={open}
      title="Track a new habit"
      onClose={() => {
        reset();
        onClose();
      }}
    >
      <form className="field" style={{ gap: "var(--space-lg)" }} onSubmit={handleSubmit}>
        <div className="field">
          <label className="field__label" htmlFor="habit-name">
            Habit name
          </label>
          <input
            id="habit-name"
            className="control"
            placeholder="Push ups"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={100}
            required
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="habit-description">
            Description
            <span style={{ color: "var(--color-muted)", fontWeight: 400 }}> (optional)</span>
          </label>
          <textarea
            id="habit-description"
            className="control"
            placeholder="What are you building here?"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={500}
          />
        </div>

        <div className="field">
          <span className="field__label">How will you log it?</span>
          <div className="field-type-grid">
            {FIELD_TYPES.map((type) => (
              <button
                key={type.value}
                type="button"
                className={`field-type-option${fieldType === type.value ? " field-type-option--active" : ""}`}
                onClick={() => setFieldType(type.value)}
                aria-pressed={fieldType === type.value}
              >
                <span className="field-type-option__label">{type.label}</span>
                <span className="field-type-option__hint">{type.hint}</span>
              </button>
            ))}
          </div>
        </div>

        {fieldType !== "BOOLEAN" && (
          <div className="field">
            <label className="field__label" htmlFor="habit-unit">
              Unit <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              id="habit-unit"
              className="control"
              placeholder={fieldType === "DURATION" ? "Min" : "Reps"}
              value={unit}
              onChange={(event) => setUnit(event.target.value)}
              maxLength={30}
            />
          </div>
        )}

        {goalAllowed && (
          <div className="field">
            <label className="field__label" htmlFor="habit-goal">
              Daily goal <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              id="habit-goal"
              className="control"
              type="number"
              inputMode="decimal"
              placeholder="e.g. 20"
              value={goalValue}
              onChange={(event) => setGoalValue(event.target.value)}
            />
            <p className="field__helper">Leave blank if any logged value should count as done.</p>
          </div>
        )}

        <p className="field__helper field__helper--error" aria-live="polite">
          {error ?? ""}
        </p>

        <button type="submit" className="btn btn--primary" disabled={status === "saving" || name.trim() === ""}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : "Start tracking"}
        </button>
      </form>
    </Modal>
  );
}
