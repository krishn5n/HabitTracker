import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { EntryResponse, HabitFieldResponse, Page } from "../lib/types";
import { SegmentedToggle } from "../components/SegmentedToggle";
import { MonthCalendarGrid } from "../components/MonthCalendarGrid";
import { EntryHistoryList } from "../components/EntryHistoryList";
import { DetailStatsRow } from "../components/DetailStatsRow";
import { Skeleton, SkeletonRegion, SkeletonSummaryRow } from "../components/Skeleton";
import "./FieldDetailPage.css";

const todayIso = () => new Date().toISOString().slice(0, 10);

const LAST_WEEK_DAYS = 7;

function lastWeekBounds(): { fromDate: string; toDate: string } {
  const start = new Date();
  start.setDate(start.getDate() - (LAST_WEEK_DAYS - 1));
  return { fromDate: start.toISOString().slice(0, 10), toDate: todayIso() };
}

function monthBounds(month: Date): { fromDate: string; toDate: string } {
  const year = month.getFullYear();
  const monthIndex = month.getMonth();
  const start = new Date(year, monthIndex, 1);
  const end = new Date(year, monthIndex + 1, 0);
  return { fromDate: start.toISOString().slice(0, 10), toDate: end.toISOString().slice(0, 10) };
}

function computeStreak(entries: EntryResponse[]): number {
  const byDate = new Map(entries.map((e) => [e.entryDate, e.completed]));
  let streak = 0;
  const cursor = new Date();
  for (;;) {
    const iso = cursor.toISOString().slice(0, 10);
    if (byDate.get(iso) !== true) break;
    streak += 1;
    cursor.setDate(cursor.getDate() - 1);
  }
  return streak;
}

export function FieldDetailPage() {
  const { habitSeqId, habitFieldSeqId } = useParams();
  const navigate = useNavigate();
  const { push } = useToast();

  const [field, setField] = useState<HabitFieldResponse | null>(null);
  const [lastWeekEntries, setLastWeekEntries] = useState<EntryResponse[] | null>(null);
  const [streak, setStreak] = useState(0);
  const [view, setView] = useState<"calendar" | "history">("calendar");

  const [month, setMonth] = useState(() => new Date());
  const [monthEntries, setMonthEntries] = useState<EntryResponse[]>([]);

  const [historyEntries, setHistoryEntries] = useState<EntryResponse[]>([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyHasMore, setHistoryHasMore] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);

  useEffect(() => {
    if (!habitFieldSeqId) return;
    api
      .get<HabitFieldResponse>(`/api/habit-fields/${habitFieldSeqId}`)
      .then(setField)
      .catch((err) => {
        if (err instanceof ApiError && (err.status === 403 || err.status === 404)) {
          push({ tone: "error", message: "That field wasn't found." });
          navigate(`/habits/${habitSeqId}`);
          return;
        }
        push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load that field." });
      });
    api
      .get<Page<EntryResponse>>(`/api/habit-fields/${habitFieldSeqId}/entries`, {
        ...lastWeekBounds(),
        size: LAST_WEEK_DAYS,
      })
      .then((res) => setLastWeekEntries(res.content))
      .catch(() => {});
    api
      .get<Page<EntryResponse>>(`/api/habit-fields/${habitFieldSeqId}/entries`, {
        toDate: todayIso(),
        size: 60,
        sort: "entryDate,desc",
      })
      .then((res) => setStreak(computeStreak(res.content)))
      .catch(() => {});
  }, [habitFieldSeqId, habitSeqId, navigate, push]);

  useEffect(() => {
    if (!habitFieldSeqId) return;
    const { fromDate, toDate } = monthBounds(month);
    api
      .get<Page<EntryResponse>>(`/api/habit-fields/${habitFieldSeqId}/entries`, { fromDate, toDate, size: 31 })
      .then((res) => setMonthEntries(res.content))
      .catch((err) => push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load the calendar." }));
  }, [habitFieldSeqId, month, push]);

  const loadHistoryPage = useCallback(
    (page: number) => {
      if (!habitFieldSeqId || historyLoading) return;
      setHistoryLoading(true);
      api
        .get<Page<EntryResponse>>(`/api/habit-fields/${habitFieldSeqId}/entries`, { page, size: 10 })
        .then((res) => {
          setHistoryEntries((current) => (page === 0 ? res.content : [...current, ...res.content]));
          setHistoryHasMore(!res.last);
          setHistoryPage(page);
        })
        .catch((err) => push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't load history." }))
        .finally(() => setHistoryLoading(false));
    },
    [habitFieldSeqId, historyLoading, push],
  );

  useEffect(() => {
    if (view === "history" && historyEntries.length === 0) loadHistoryPage(0);
  }, [view, historyEntries.length, loadHistoryPage]);

  if (!field) {
    return (
      <SkeletonRegion label="Loading this field" className="field-detail">
        <Link to={`/habits/${habitSeqId}`} className="field-detail__back">
          ← Back to habit
        </Link>
        <div className="skeleton-stack">
          <Skeleton width="55%" height="2.25rem" />
          <Skeleton width="30%" height="0.875rem" />
        </div>
        <SkeletonSummaryRow />
        <Skeleton width="11.5rem" height="2.625rem" radius="md" />
        <Skeleton height="20rem" radius="lg" />
      </SkeletonRegion>
    );
  }

  const rate =
    lastWeekEntries && lastWeekEntries.length > 0
      ? Math.round((lastWeekEntries.filter((e) => e.completed).length / lastWeekEntries.length) * 100)
      : null;
  const now = new Date();
  const isCurrentMonth = month.getFullYear() === now.getFullYear() && month.getMonth() === now.getMonth();

  return (
    <section className="field-detail">
      <Link to={`/habits/${habitSeqId}`} className="field-detail__back">
        ← Back to habit
      </Link>

      <div className="field-detail__head">
        <div>
          <h1 className="field-detail__title">{field.fieldName}</h1>
          <p className="field-detail__meta">
            {field.fieldType}
            {field.unit ? ` · ${field.unit}` : ""}
            {field.goalValue !== null ? ` · goal ${field.goalValue}` : ""}
          </p>
        </div>
      </div>

      <DetailStatsRow
        streak={streak}
        rateLabel={rate !== null ? `${rate}%` : "—"}
        goalLabel={field.goalValue !== null ? `${field.goalValue}${field.unit ? ` ${field.unit}` : ""}` : "Any"}
      />

      <div className="field-detail__toggle-wrap">
        <SegmentedToggle
          options={[
            { value: "calendar", label: "Calendar" },
            { value: "history", label: "History" },
          ]}
          value={view}
          onChange={setView}
        />
      </div>

      <div className="field-detail__section">
        {view === "calendar" ? (
          <MonthCalendarGrid
            month={month}
            entries={monthEntries}
            onPrevMonth={() => setMonth((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1))}
            onNextMonth={() => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1))}
            disableNext={isCurrentMonth}
          />
        ) : (
          <EntryHistoryList
            entries={historyEntries}
            unit={field.unit}
            goalValue={field.goalValue}
            onScrollEnd={() => {
              if (historyHasMore && !historyLoading) loadHistoryPage(historyPage + 1);
            }}
          />
        )}
      </div>
    </section>
  );
}
