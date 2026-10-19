import { Link } from "react-router-dom";
import type { DashboardHabit, EntryResponse } from "../lib/types";
import { HabitTile, fieldStatus } from "./HabitTile";
import "./HabitGroupTile.css";

interface HabitGroupTileProps {
  habit: DashboardHabit;
  onLogged: (entry: EntryResponse) => void;
}

export function HabitGroupTile({ habit, onLogged }: HabitGroupTileProps) {
  const done = habit.fields.filter((f) => fieldStatus(f) === "achieved").length;

  return (
    <div className="habit-group-tile">
      <div className="habit-group-tile__head">
        <h3 className="habit-group-tile__name">
          <Link to={`/habits/${habit.habitSeqId}`}>{habit.name}</Link>
        </h3>
        <span className="habit-group-tile__progress">
          {done} of {habit.fields.length} done
        </span>
      </div>
      <div className="habit-group-tile__fields">
        {habit.fields.map((field) => (
          <HabitTile
            key={field.habitFieldSeqId}
            habitSeqId={habit.habitSeqId}
            field={field}
            onLogged={onLogged}
          />
        ))}
      </div>
    </div>
  );
}
