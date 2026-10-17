import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { HabitResponse, Page } from "../lib/types";
import { EmptyState } from "../components/EmptyState";
import { CreateHabitModal } from "../components/CreateHabitModal";
import { EditHabitModal } from "../components/EditHabitModal";
import "./HabitsPage.css";

export function HabitsPage() {
  const { session } = useAuth();
  const { push } = useToast();
  const [habits, setHabits] = useState<HabitResponse[] | null>(null);
  const [showInactive, setShowInactive] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<HabitResponse | null>(null);

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
              <div>
                <Link className="habits-list__name" to={`/habits/${habit.habitSeqId}`}>
                  {habit.name}
                </Link>
                {habit.description && <p className="habits-list__description">{habit.description}</p>}
                {!habit.active && <span className="habits-list__badge">Removed</span>}
              </div>
              <div className="habits-list__actions">
                <button type="button" className="btn btn--outline btn--sm" onClick={() => setEditing(habit)}>
                  Edit
                </button>
                {habit.active && (
                  <button type="button" className="btn btn--danger btn--sm" onClick={() => handleDelete(habit)}>
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
    </section>
  );
}
