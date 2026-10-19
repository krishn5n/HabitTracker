import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type {
  DashboardField,
  DashboardResponse,
  EntryResponse,
  HabitFieldResponse,
  HabitResponse,
} from "../lib/types";
import { HabitTile, fieldStatus } from "../components/HabitTile";
import { HabitGroupTile } from "../components/HabitGroupTile";
import { SummaryRow } from "../components/SummaryRow";
import { EmptyState } from "../components/EmptyState";
import { CreateHabitModal } from "../components/CreateHabitModal";
import { Skeleton, SkeletonRegion, SkeletonSummaryRow } from "../components/Skeleton";
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

  function handleLogged(entry: EntryResponse) {
    if (entry.entryDate !== new Date().toISOString().slice(0, 10)) return;
    setData((current) =>
      current === null
        ? current
        : {
            habits: current.habits.map((habit) => ({
              ...habit,
              fields: habit.fields.map((field) =>
                field.habitFieldSeqId === entry.habitFieldSeqId
                  ? { ...field, todayValue: entry.entryValue, completedToday: entry.completed, updatedAt: entry.updatedAt }
                  : field,
              ),
            })),
          },
    );
  }

  const allFields: DashboardField[] =data?.habits.flatMap((h) => h.fields) ?? [];
  const achieved = allFields.filter((f) => fieldStatus(f) === "achieved").length;
  const partial = allFields.filter((f) => fieldStatus(f) === "partial").length;
  const empty = allFields.filter((f) => fieldStatus(f) === "empty").length;
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

      {status === "loading" && (
        <SkeletonRegion label="Loading your habits" className="dashboard__loading">
          <SkeletonSummaryRow />
          <div className="dashboard__grid">
            {[0, 1, 2].map((i) => (
              <div key={i} className="habit-tile">
                <Skeleton width="45%" height="0.875rem" />
                <Skeleton width="35%" height="3rem" />
                <Skeleton height="var(--control-height)" radius="md" />
              </div>
            ))}
          </div>
        </SkeletonRegion>
      )}

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
        <>
          <SummaryRow achieved={achieved} partial={partial} empty={empty} />
          <div className="dashboard__grid">
            {data.habits.map((habit) =>
              habit.fields.length > 1 ? (
                <HabitGroupTile key={habit.habitSeqId} habit={habit} onLogged={handleLogged} />
              ) : (
                <HabitTile
                  key={habit.habitSeqId}
                  habitSeqId={habit.habitSeqId}
                  field={habit.fields[0]}
                  wide
                  onLogged={handleLogged}
                />
              ),
            )}
          </div>
        </>
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
