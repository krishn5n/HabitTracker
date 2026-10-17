import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { Gender, UserResponse } from "../lib/types";
import "./SettingsPage.css";

const GENDERS: Gender[] = ["UNSPECIFIED", "FEMALE", "MALE", "OTHER"];

export function SettingsPage() {
  const { session } = useAuth();
  const { push } = useToast();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [gender, setGender] = useState<Gender>("UNSPECIFIED");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [weightKg, setWeightKg] = useState("");
  const [heightCm, setHeightCm] = useState("");
  const [status, setStatus] = useState<"idle" | "saving">("idle");

  useEffect(() => {
    if (!session) return;
    api.get<UserResponse>(`/api/users/${session.userSeqId}`).then((res) => {
      setUser(res);
      setDisplayName(res.displayName);
      setGender(res.gender ?? "UNSPECIFIED");
      setDateOfBirth(res.dateOfBirth ?? "");
      setWeightKg(res.weightKg?.toString() ?? "");
      setHeightCm(res.heightCm?.toString() ?? "");
    });
  }, [session]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!session) return;
    setStatus("saving");
    try {
      const updated = await api.put<UserResponse>(`/api/users/${session.userSeqId}`, {
        displayName: displayName.trim() === "" ? undefined : displayName.trim(),
        gender,
        dateOfBirth: dateOfBirth === "" ? undefined : dateOfBirth,
        weightKg: weightKg === "" ? undefined : Number(weightKg),
        heightCm: heightCm === "" ? undefined : Number(heightCm),
      });
      setUser(updated);
      push({ tone: "info", message: "Profile saved." });
    } catch (err) {
      push({ tone: "error", message: err instanceof ApiError ? err.detail : "Couldn't save your profile." });
    } finally {
      setStatus("idle");
    }
  }

  if (!user) return <p className="field__helper">Loading…</p>;

  return (
    <section className="settings-page">
      <h1 className="settings-page__title">Your profile</h1>
      <p className="settings-page__meta">
        {user.email}
        {user.age !== null ? ` · ${user.age} years old` : ""}
      </p>

      <form className="settings-form" onSubmit={handleSubmit}>
        <div className="field">
          <label className="field__label" htmlFor="display-name">
            Display name
          </label>
          <input
            id="display-name"
            className="control"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            maxLength={100}
          />
        </div>

        <div className="field">
          <label className="field__label" htmlFor="gender">
            Gender
          </label>
          <select id="gender" className="control" value={gender} onChange={(event) => setGender(event.target.value as Gender)}>
            {GENDERS.map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label className="field__label" htmlFor="dob">
            Date of birth
          </label>
          <input id="dob" className="control" type="date" value={dateOfBirth} onChange={(event) => setDateOfBirth(event.target.value)} />
        </div>

        <div className="settings-form__row">
          <div className="field">
            <label className="field__label" htmlFor="weight">
              Weight (kg)
            </label>
            <input
              id="weight"
              className="control"
              type="number"
              inputMode="decimal"
              value={weightKg}
              onChange={(event) => setWeightKg(event.target.value)}
            />
          </div>
          <div className="field">
            <label className="field__label" htmlFor="height">
              Height (cm)
            </label>
            <input
              id="height"
              className="control"
              type="number"
              inputMode="decimal"
              value={heightCm}
              onChange={(event) => setHeightCm(event.target.value)}
            />
          </div>
        </div>

        <button type="submit" className="btn btn--primary" disabled={status === "saving"}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : "Save changes"}
        </button>
      </form>
    </section>
  );
}
