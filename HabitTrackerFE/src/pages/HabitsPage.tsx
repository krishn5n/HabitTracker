import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { HabitResponse, Page } from "../lib/types";
import { EmptyState } from "../components/EmptyState";
import { CreateHabitModal } from "../components/CreateHabitModal";
import { EditHabitModal } from "../components/EditHabitModal";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Skeleton, SkeletonRegion } from "../components/Skeleton";
import "./HabitsPage.css";

export function HabitsPage() {
  const { session } = useAuth();
  const { push } = useToast();
  const [habits, setHabits] = useState<HabitResponse[] | null>(null);
  const [showInactive, setShowInactive] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<HabitResponse | null>(null);
  const [removing, setRemoving] = useState<HabitResponse | null>(null);

  useEffect(() => {
    if (!session) return;
    api
      .get<Page<HabitResponse>>("/api/habits", { userSeqId: session.userSeqId, activeOnly: !showInactive, size: 100 })
      .then((res) => setHabits(res.content))
      .catch((err) => {
        push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load your habits." });
      });
  }, [session, showInactive, push]);

  async function handleDelete(habit: HabitResponse) {
    try {
      await api.delete(`/api/habits/${habit.habitSeqId}`);
      setHabits((current) => current?.map((h) => (h.habitSeqId === habit.habitSeqId ? { ...h, active: false } : h)) ?? null);
    } catch (err) {
      push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't remove that habit." });
    } finally {
      setRemoving(null);
    }
  }

  function handleCreated(habit: HabitResponse) {
    setHabits((current) => [...(current ?? []), habit]);
  }

  function handleUpdated(habit: HabitResponse) {
    setHabits((current) => current?.map((h) => (h.habitSeqId === habit.habitSeqId ? habit : h)) ?? null);
    setEditing(null);
  }

  return (
    <section className="habits-page">
      <div className="habits-page__head">
        <h1 className="habits-page__title">Your habits</h1>
        <button type="button" className="btn btn--primary" onClick={() => setCreateOpen(true)}>
          New habit
        </button>
      </div>

      <label className="habits-page__toggle">
        <input type="checkbox" checked={showInactive} onChange={(event) => setShowInactive(event.target.checked)} />
        Show removed habits
      </label>

      {habits === null && <HabitsListSkeleton />}

      {habits && habits.length === 0 && (
        <EmptyState
          title="No habits yet"
          body="Create a habit, pick how you'll log it, and it shows up on today's dashboard."
          actionLabel="Track your first habit"
          onAction={() => setCreateOpen(true)}
        />
      )}

      {habits && habits.length > 0 && (
        <ul className="habits-list">
          {habits.map((habit) => (
            <li key={habit.habitSeqId} className={`habits-list__row${habit.active ? "" : " habits-list__row--inactive"}`}>
              <Link
                className="habits-list__link"
                to={`/habits/${habit.habitSeqId}`}
                aria-label={`Open ${habit.name}`}
              />
              <div>
                <span className="habits-list__name">{habit.name}</span>
                {habit.description && <p className="habits-list__description">{habit.description}</p>}
                {!habit.active && <span className="habits-list__badge">Removed</span>}
              </div>
              <div className="habits-list__actions">
                <button type="button" className="btn btn--outline btn--sm" onClick={() => setEditing(habit)}>
                  Edit
                </button>
                {habit.active && (
                  <button type="button" className="btn btn--danger btn--sm" onClick={() => setRemoving(habit)}>
                    Remove
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {session && (
        <CreateHabitModal
          open={createOpen}
          userSeqId={session.userSeqId}
          onClose={() => setCreateOpen(false)}
          onCreated={handleCreated}
        />
      )}

      {editing && <EditHabitModal habit={editing} onClose={() => setEditing(null)} onUpdated={handleUpdated} />}

      {removing && (
        <ConfirmDialog
          title="Remove this habit?"
          message={`"${removing.name}" will disappear from today's dashboard. Its past entries are kept, and you can still find it under "Show removed habits".`}
          confirmLabel="Remove"
          onConfirm={() => handleDelete(removing)}
          onCancel={() => setRemoving(null)}
        />
      )}
    </section>
  );
}

function HabitsListSkeleton() {
  return (
    <SkeletonRegion label="Loading your habits">
      <ul className="habits-list">
        {[0, 1, 2, 3].map((i) => (
          <li key={i} className="habits-list__row">
            <div className="skeleton-stack habits-list__skeleton-text">
              <Skeleton width={i % 2 === 0 ? "55%" : "40%"} height="1.25rem" />
              <Skeleton width="80%" height="0.875rem" />
            </div>
            <div className="habits-list__actions">
              <Skeleton width="3.5rem" height="2.25rem" radius="md" />
              <Skeleton width="5rem" height="2.25rem" radius="md" />
            </div>
          </li>
        ))}
      </ul>
    </SkeletonRegion>
  );
}
