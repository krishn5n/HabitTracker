import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { HabitFieldResponse, HabitResponse, Page } from "../lib/types";
import { FieldPanel, FieldPanelSkeleton } from "../components/FieldPanel";
import { Skeleton, SkeletonRegion } from "../components/Skeleton";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { EditHabitModal } from "../components/EditHabitModal";
import { HabitFieldFormModal } from "../components/HabitFieldFormModal";
import "./HabitDetailPage.css";

export function HabitDetailPage() {
  const { habitSeqId } = useParams();
  const navigate = useNavigate();
  const { push } = useToast();
  const [habit, setHabit] = useState<HabitResponse | null>(null);
  const [fields, setFields] = useState<HabitFieldResponse[] | null>(null);
  const [editingHabit, setEditingHabit] = useState(false);
  const [fieldModal, setFieldModal] = useState<"new" | HabitFieldResponse | null>(null);
  const [deletingField, setDeletingField] = useState<HabitFieldResponse | null>(null);

  useEffect(() => {
    if (!habitSeqId) return;
    api
      .get<HabitResponse>(`/api/habits/${habitSeqId}`)
      .then(setHabit)
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
          push({ tone: "error", message: "That habit wasn't found." });
          navigate("/habits");
          return;
        }
        push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load that habit." });
      });
    api
      .get<Page<HabitFieldResponse>>(`/api/habits/${habitSeqId}/fields`, { size: 50 })
      .then((res) => setFields(res.content))
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 403 || err.status === 404)) return;
        push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load its fields." });
      });
  }, [habitSeqId, navigate, push]);

  async function handleDeleteField(field: HabitFieldResponse) {
    try {
      await api.delete(`/api/habit-fields/${field.habitFieldSeqId}`);
      setFields((current) => current?.filter((f) => f.habitFieldSeqId !== field.habitFieldSeqId) ?? null);
    } catch (err) {
      push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't remove that field." });
    } finally {
      setDeletingField(null);
    }
  }

  if (!habit || !fields) {
    return (
      <SkeletonRegion label="Loading this habit" className="habit-detail">
        <Link to="/habits" className="habit-detail__back">
          ← All habits
        </Link>
        <div className="habit-detail__head">
          <div className="skeleton-stack" style={{ flex: 1 }}>
            <Skeleton width="60%" height="2.25rem" />
            <Skeleton width="85%" height="1rem" />
          </div>
        </div>
        <div className="habit-detail__fields">
          <FieldPanelSkeleton />
          <FieldPanelSkeleton />
        </div>
      </SkeletonRegion>
    );
  }

  return (
    <section className="habit-detail">
      <Link to="/habits" className="habit-detail__back">
        ← All habits
      </Link>

      <div className="habit-detail__head">
        <div>
          <h1 className="habit-detail__title">{habit.name}</h1>
          {habit.description && <p className="habit-detail__description">{habit.description}</p>}
        </div>
        <button type="button" className="btn btn--outline btn--sm" onClick={() => setEditingHabit(true)}>
          Edit habit
        </button>
      </div>

      <div className="habit-detail__fields">
        {fields.map((field) => (
          <FieldPanel
            key={field.habitFieldSeqId}
            field={field}
            onEdit={() => setFieldModal(field)}
            onDelete={() => setDeletingField(field)}
          />
        ))}
        <button type="button" className="btn btn--outline" onClick={() => setFieldModal("new")}>
          Add another field
        </button>
      </div>

      {editingHabit && (
        <EditHabitModal
          habit={habit}
          onClose={() => setEditingHabit(false)}
          onUpdated={(updated) => {
            setHabit(updated);
            setEditingHabit(false);
          }}
        />
      )}

      {fieldModal && (
        <HabitFieldFormModal
          habitSeqId={habit.habitSeqId}
          field={fieldModal === "new" ? null : fieldModal}
          onClose={() => setFieldModal(null)}
          onSaved={(saved) => {
            setFields((current) => {
              if (!current) return current;
              const exists = current.some((f) => f.habitFieldSeqId === saved.habitFieldSeqId);
              return exists
                ? current.map((f) => (f.habitFieldSeqId === saved.habitFieldSeqId ? saved : f))
                : [...current, saved];
            });
            setFieldModal(null);
          }}
        />
      )}

      {deletingField && (
        <ConfirmDialog
          title="Delete this sub habit?"
          message={`"${deletingField.fieldName}" will be deleted permanently. This can't be undone.`}
          confirmLabel="Delete"
          onConfirm={() => handleDeleteField(deletingField)}
          onCancel={() => setDeletingField(null)}
        />
      )}

      {!habit.active && (
        <p className="habit-detail__removed">
          This habit is removed.{" "}
          <button type="button" className="btn btn--ghost btn--sm" onClick={() => navigate("/habits")}>
            Back to habits
          </button>
        </p>
      )}
    </section>
  );
}
