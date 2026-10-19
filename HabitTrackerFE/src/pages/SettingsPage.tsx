import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api, ApiError } from "../lib/api";
import type { Gender, UserResponse } from "../lib/types";
import { Skeleton, SkeletonField, SkeletonRegion } from "../components/Skeleton";
import "./SettingsPage.css";

const GENDERS: Gender[] = ["UNSPECIFIED", "FEMALE", "MALE", "OTHER"];
const GENDER_LABELS: Record<Gender, string> = {
  UNSPECIFIED: "Unspecified",
  FEMALE: "Female",
  MALE: "Male",
  OTHER: "Other",
};

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

  if (!user) {
    return (
      <SkeletonRegion label="Loading your profile" className="settings-page">
        <Skeleton width="60%" height="2.25rem" />
        <Skeleton width="45%" height="0.875rem" />
        <div className="settings-form">
          <SkeletonField labelWidth="30%" />
          <SkeletonField labelWidth="20%" />
          <SkeletonField labelWidth="30%" />
          <div className="settings-form__row">
            <SkeletonField labelWidth="45%" />
            <SkeletonField labelWidth="45%" />
          </div>
          <Skeleton height="var(--control-height)" radius="md" />
        </div>
      </SkeletonRegion>
    );
  }

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
                {GENDER_LABELS[value]}
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
              Weight<span className="visually-hidden"> in kilograms</span>
            </label>
            <div className="settings-form__unit-control">
              <input
                id="weight"
                className="control"
                type="number"
                inputMode="decimal"
                step="any"
                min={0}
                placeholder="0"
                value={weightKg}
                onChange={(event) => setWeightKg(event.target.value)}
                onFocus={(event) => event.currentTarget.scrollIntoView({ block: "center", behavior: "smooth" })}
              />
              <span className="settings-form__unit" aria-hidden>
                kg
              </span>
            </div>
          </div>
          <div className="field">
            <label className="field__label" htmlFor="height">
              Height<span className="visually-hidden"> in centimetres</span>
            </label>
            <div className="settings-form__unit-control">
              <input
                id="height"
                className="control"
                type="number"
                inputMode="decimal"
                step="any"
                min={0}
                placeholder="0"
                value={heightCm}
                onChange={(event) => setHeightCm(event.target.value)}
                onFocus={(event) => event.currentTarget.scrollIntoView({ block: "center", behavior: "smooth" })}
              />
              <span className="settings-form__unit" aria-hidden>
                cm
              </span>
            </div>
          </div>
        </div>

        <button type="submit" className="btn btn--primary settings-form__submit" disabled={status === "saving"}>
          {status === "saving" ? <span className="spinner" aria-hidden /> : "Save changes"}
        </button>
      </form>
    </section>
  );
}
