import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { getToken, setToken } from "../lib/api";

interface Session {
  userSeqId: number;
  email: string;
}

interface AuthContextValue {
  session: Session | null;
  signIn: (session: Session, token: string) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const SESSION_KEY = "habit-tracker-session";

function readStoredSession(): Session | null {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw || !getToken()) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readStoredSession());

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      signIn: (next, token) => {
        setToken(token);
        sessionStorage.setItem(SESSION_KEY, JSON.stringify(next));
        setSession(next);
      },
      signOut: () => {
        setToken(null);
        sessionStorage.removeItem(SESSION_KEY);
        setSession(null);
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
