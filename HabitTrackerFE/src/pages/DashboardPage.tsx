import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { DashboardResponse, EntryResponse, HabitFieldResponse, HabitResponse } from "../lib/types";
import { HabitCard } from "../components/HabitCard";
import { StatTile } from "../components/StatTile";
import { EmptyState } from "../components/EmptyState";
import { CreateHabitModal } from "../components/CreateHabitModal";
import "./DashboardPage.css";

export function DashboardPage() {
  const { session } = useAuth();
  const { push } = useToast();
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    let active = true;
    api
      .get<DashboardResponse>("/api/dashboard")
      .then((res) => {
        if (active) {
          setData(res);
          setStatus("ready");
        }
      })
      .catch((err) => {
        if (!active) return;
        setStatus("error");
        push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load your dashboard." });
      });
    return () => {
      active = false;
    };
  }, [push]);

  function handleFieldLogged(habitFieldSeqId: number, entry: EntryResponse) {
    setData((current) => {
      if (!current) return current;
      return {
        habits: current.habits.map((habit) => ({
          ...habit,
          fields: habit.fields.map((field) =>
            field.habitFieldSeqId === habitFieldSeqId
              ? {
                  ...field,
                  completedToday: entry.completed,
                  todayValue: entry.entryValue,
                  updatedAt: entry.updatedAt,
                }
              : field,
          ),
        })),
      };
    });
  }

  function handleCreated(habit: HabitResponse, field: HabitFieldResponse) {
    setData((current) => ({
      habits: [
        ...(current?.habits ?? []),
        {
          habitSeqId: habit.habitSeqId,
          name: habit.name,
          fields: [
            {
              habitFieldSeqId: field.habitFieldSeqId,
              fieldName: field.fieldName,
              fieldType: field.fieldType,
              unit: field.unit,
              goalValue: field.goalValue,
              completedToday: false,
              todayValue: null,
              updatedAt: null,
            },
          ],
        },
      ],
    }));
  }

  const totalFields = data?.habits.flatMap((h) => h.fields).length ?? 0;
  const completedFields = data?.habits.flatMap((h) => h.fields).filter((f) => f.completedToday).length ?? 0;
  const today = new Date().toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" });

  return (
    <section className="dashboard">
      <div className="dashboard__head">
        <div>
          <p className="dashboard__eyebrow">{today}</p>
          <h1 className="dashboard__title">Today's entry</h1>
        </div>
        <button type="button" className="btn btn--primary" onClick={() => setModalOpen(true)}>
          New habit
        </button>
      </div>

      {status === "loading" && <p className="field__helper">Loading your habits…</p>}

      {status === "ready" && data && data.habits.length === 0 && (
        <div className="dashboard__grid">
          <EmptyState
            title="No habits yet"
            body="Habits are the things you want to track — push ups, water, reading. Add one and log today's value in under ten seconds."
            actionLabel="Track your first habit"
            onAction={() => setModalOpen(true)}
          />
        </div>
      )}

      {status === "ready" && data && data.habits.length > 0 && (
        <div className="dashboard__grid">
          <StatTile value={completedFields} total={totalFields} label="Habits logged today" />
          {data.habits.map((habit) => (
            <HabitCard key={habit.habitSeqId} habit={habit} onFieldLogged={handleFieldLogged} />
          ))}
        </div>
      )}

      {session && (
        <CreateHabitModal
          open={modalOpen}
          userSeqId={session.userSeqId}
          onClose={() => setModalOpen(false)}
          onCreated={handleCreated}
        />
      )}
    </section>
  );
}
