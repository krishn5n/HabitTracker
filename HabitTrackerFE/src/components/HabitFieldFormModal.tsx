import { useState, type FormEvent } from "react";
import { Modal } from "./Modal";
import { api, ApiError } from "../lib/api";
import type { FieldType, HabitFieldResponse } from "../lib/types";
import "./CreateHabitModal.css";

interface HabitFieldFormModalProps {
  habitSeqId: number;
  field: HabitFieldResponse | null;
  onClose: () => void;
  onSaved: (field: HabitFieldResponse) => void;
}

const FIELD_TYPES: { value: FieldType; label: string }[] = [
  { value: "NUMBER", label: "Number" },
  { value: "DURATION", label: "Duration" },
  { value: "BOOLEAN", label: "Yes / no" },
  { value: "TEXT", label: "Note" },
];

export function HabitFieldFormModal({ habitSeqId, field, onClose, onSaved }: HabitFieldFormModalProps) {
  const isEdit = field !== null;
  const [fieldName, setFieldName] = useState(field?.fieldName ?? "");
  const [fieldType, setFieldType] = useState<FieldType>(field?.fieldType ?? "NUMBER");
  const [unit, setUnit] = useState(field?.unit ?? "");
  const [goalValue, setGoalValue] = useState(field?.goalValue?.toString() ?? "");
  const [status, setStatus] = useState<"idle" | "saving" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  const goalAllowed = fieldType === "NUMBER" || fieldType === "DURATION";

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setStatus("saving");
    setError(null);
    try {
      const saved = isEdit
        ? await api.put<HabitFieldResponse>(`/api/habit-fields/${field!.habitFieldSeqId}`, {
            fieldName: fieldName.trim() === "" ? undefined : fieldName.trim(),
            unit: unit.trim() === "" ? undefined : unit.trim(),
            goalValue: goalAllowed && goalValue.trim() !== "" ? Number(goalValue) : undefined,
          })
        : await api.post<HabitFieldResponse>(`/api/habits/${habitSeqId}/fields`, {
            fieldName: fieldName.trim() === "" ? undefined : fieldName.trim(),
            fieldType,
            unit: unit.trim() === "" ? undefined : unit.trim(),
            goalValue: goalAllowed && goalValue.trim() !== "" ? Number(goalValue) : undefined,
          });
      onSaved(saved);
    } catch (err) {
      setStatus("error");
      setError(err instanceof ApiError ? err.detail : "Couldn't save that field.");
    }
  }

  return (
    <Modal open title={isEdit ? "Edit field" : "Add a field"} onClose={onClose}>
      <form className="field" style={{ gap: "var(--space-lg)" }} onSubmit={handleSubmit}>
        <div className="field">
          <label className="field__label" htmlFor="field-name">
            Field name <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>(optional)</span>
          </label>
          <input
            id="field-name"
            className="control"
            placeholder="Defaults to the habit name"
            value={fieldName}
            onChange={(event) => setFieldName(event.target.value)}
            maxLength={100}
          />
        </div>

        {!isEdit && (
          <div className="field">
            <span className="field__label">Type</span>
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
                </button>
              ))}
            </div>
          </div>
        )}

        {fieldType !== "BOOLEAN" && (
          <div className="field">
            <label className="field__label" htmlFor="field-unit">
              Unit <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              id="field-unit"
              className="control"
              value={unit}
              onChange={(event) => setUnit(event.target.value)}
              maxLength={30}
            />
          </div>
        )}

        {goalAllowed && (
          <div className="field">
            <label className="field__label" htmlFor="field-goal">
              Daily goal <span style={{ color: "var(--color-muted)", fontWeight: 400 }}>(optional)</span>
            </label>
            <input
              id="field-goal"
              className="control"
              type="number"
              inputMode="decimal"
              value={goalValue}
              onChange={(event) => setGoalValue(event.target.value)}
            />
          </div>
        )}

        <p className="field__helper field__helper--error" aria-live="polite">
          {error ?? ""}
        </p>

        <button type="submit" className="btn btn--primary" disabled={status === "saving"}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : isEdit ? "Save changes" : "Add field"}
        </button>
      </form>
    </Modal>
  );
}
