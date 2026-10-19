import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api, ApiError } from "../lib/api";
import { createCredential, isWebAuthnSupported, type RegistrationOptionsResponse } from "../lib/webauthn";
import "./AuthPage.css";

export function RegisterPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [status, setStatus] = useState<"idle" | "working" | "error">("idle");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!isWebAuthnSupported()) {
      setError("This browser doesn't support passkeys. Try the latest Chrome, Safari, or Edge.");
      return;
    }
    setStatus("working");
    setError(null);
    try {
      const options = await api.post<RegistrationOptionsResponse>("/api/auth/webauthn/registration/options", {
        email: email.trim(),
        displayName: displayName.trim(),
      });
      const credential = await createCredential(options);
      await api.post("/api/auth/webauthn/registration/verify", {
        email: email.trim(),
        displayName: displayName.trim(),
        dateOfBirth,
        ...credential,
      });
      navigate("/login", { state: { registered: true, email: email.trim() } });
    } catch (err) {
      setStatus("error");
      if (err instanceof ApiError) setError(err.detail);
      else if (err instanceof DOMException && err.name === "NotAllowedError") {
        setError("Passkey creation was cancelled. Try again when you're ready.");
      } else {
        setError("Couldn't create your passkey. Try again.");
      }
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card">
        <div>
          <p className="auth-card__eyebrow">Nitya</p>
          <h1 className="auth-card__title">Keep the record.</h1>
          <p className="auth-card__body">
            No passwords — your device's passkey is the key. Create one, then sign in with it every day after.
          </p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="field">
            <label className="field__label" htmlFor="register-name">
              Your name
            </label>
            <input
              id="register-name"
              name="name"
              className="control"
              autoComplete="name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              maxLength={100}
              required
            />
          </div>
          <div className="field">
            <label className="field__label" htmlFor="register-dob">
              Date of birth
            </label>
            <input
              id="register-dob"
              name="bday"
              className="control"
              type="date"
              autoComplete="bday"
              value={dateOfBirth}
              onChange={(event) => setDateOfBirth(event.target.value)}
              max={new Date().toISOString().slice(0, 10)}
              required
            />
          </div>
          <div className="field">
            <label className="field__label" htmlFor="register-email">
              Email address
            </label>
            <input
              id="register-email"
              name="email"
              className="control"
              type="email"
              autoComplete="email"
              inputMode="email"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              maxLength={255}
              required
            />
          </div>

          <p className="field__helper field__helper--error" aria-live="polite">
            {error ?? ""}
          </p>

          <button type="submit" className="btn btn--primary" disabled={status === "working"}>
            {status === "working" ? <span className="spinner" aria-hidden /> : "Create your passkey"}
          </button>
        </form>

        <p className="auth-card__switch">
          Already keeping a record? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </section>
  );
}
