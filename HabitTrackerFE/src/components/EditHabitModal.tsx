import { useState, type FormEvent } from "react";
import { Modal } from "./Modal";
import { api, ApiError } from "../lib/api";
import type { HabitResponse } from "../lib/types";

interface EditHabitModalProps {
  habit: HabitResponse;
  onClose: () => void;
  onUpdated: (habit: HabitResponse) => void;
}

export function EditHabitModal({ habit, onClose, onUpdated }: EditHabitModalProps) {
  const [name, setName] = useState(habit.name);
  const [description, setDescription] = useState(habit.description ?? "");
  const [status, setStatus] = useState<"idle" | "saving" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setStatus("saving");
    setError(null);
    try {
      const updated = await api.put<HabitResponse>(`/api/habits/${habit.habitSeqId}`, {
        name: name.trim(),
        description: description.trim() === "" ? undefined : description.trim(),
      });
      onUpdated(updated);
    } catch (err) {
      setStatus("error");
      setError(err instanceof ApiError ? err.detail : "Couldn't save those changes.");
    }
  }

  return (
    <Modal open title="Edit habit" onClose={onClose}>
      <form className="field" style={{ gap: "var(--space-lg)" }} onSubmit={handleSubmit}>
        <div className="field">
          <label className="field__label" htmlFor="edit-habit-name">
            Habit name
          </label>
          <input
            id="edit-habit-name"
            className="control"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={100}
            required
          />
        </div>
        <div className="field">
          <label className="field__label" htmlFor="edit-habit-description">
            Description
          </label>
          <textarea
            id="edit-habit-description"
            className="control"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={500}
          />
        </div>
        <p className="field__helper field__helper--error" aria-live="polite">
          {error ?? ""}
        </p>
        <button type="submit" className="btn btn--primary" disabled={status === "saving" || name.trim() === ""}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : "Save changes"}
        </button>
      </form>
    </Modal>
  );
}
