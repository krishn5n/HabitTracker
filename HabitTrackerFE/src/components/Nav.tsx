import { NavLink } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import "./Nav.css";

const TABS = [
  { to: "/", label: "Today" },
  { to: "/habits", label: "Habits" },
  { to: "/settings", label: "Settings" },
];

export function Nav() {
  const { session, signOut } = useAuth();

  return (
    <header className="nav">
      <div className="nav__bar">
        <span className="nav__wordmark">Nitya</span>
        {session && (
          <button type="button" className="btn btn--ghost btn--sm" onClick={signOut}>
            Sign out
          </button>
        )}
      </div>
      {session && (
        <nav className="nav__tabs" aria-label="Primary">
          {TABS.map((tab) => (
            <NavLink
              key={tab.to}
              to={tab.to}
              className={({ isActive }) => `nav__tab${isActive ? " nav__tab--active" : ""}`}
              end={tab.to === "/"}
            >
              {tab.label}
            </NavLink>
          ))}
        </nav>
      )}
    </header>
  );
}
