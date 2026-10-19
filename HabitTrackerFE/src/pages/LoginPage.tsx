import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { api, ApiError } from "../lib/api";
import { getCredential, isWebAuthnSupported, type LoginOptionsResponse } from "../lib/webauthn";
import "./AuthPage.css";

interface LoginVerifyResponse {
  token: string;
  userSeqId: number;
  email: string;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation() as { state?: { registered?: boolean; email?: string } };
  const { signIn } = useAuth();
  const [email, setEmail] = useState(location.state?.email ?? "");
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
      const options = await api.post<LoginOptionsResponse>("/api/auth/webauthn/login/options", {
        email: email.trim(),
      });
      const assertion = await getCredential(options);
      const result = await api.post<LoginVerifyResponse>("/api/auth/webauthn/login/verify", {
        email: email.trim(),
        ...assertion,
      });
      signIn({ userSeqId: result.userSeqId, email: result.email }, result.token);
      navigate("/", { replace: true });
    } catch (err) {
      setStatus("error");
      if (err instanceof ApiError) setError(err.detail);
      else if (err instanceof DOMException && err.name === "NotAllowedError") {
        setError("Passkey sign-in was cancelled. Try again when you're ready.");
      } else {
        setError("Couldn't sign you in. Try again.");
      }
    }
  }

  return (
    <section className="auth-page">
      <div className="auth-card">
        <div>
          <p className="auth-card__eyebrow">Nitya</p>
          <h1 className="auth-card__title">Welcome back.</h1>
          <p className="auth-card__body">
            {location.state?.registered
              ? "Your passkey is ready. Sign in to start today's record."
              : "Sign in with the passkey on this device."}
          </p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="field">
            <label className="field__label" htmlFor="login-email">
              Email address
            </label>
            <input
              id="login-email"
              name="email"
              className="control"
              type="email"
              autoComplete="username"
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
            {status === "working" ? <span className="spinner" aria-hidden /> : "Sign in with passkey"}
          </button>
        </form>

        <p className="auth-card__switch">
          New here? <Link to="/register">Create a passkey</Link>
        </p>
      </div>
    </section>
  );
}
