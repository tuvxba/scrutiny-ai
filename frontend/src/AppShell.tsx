import { ScanSearch } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "./auth";

export function AppShell() {
  const { token, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <button className="brand brand-link" type="button" onClick={() => navigate("/")}>
          <div className="brand-mark">
            <ScanSearch size={20} />
          </div>
          <div>
            <h1>ScrutinyAI</h1>
            <p>Code review lab</p>
          </div>
        </button>
        <nav className="nav">
          <NavLink to="/review">New review</NavLink>
          {token && <NavLink to="/history">History</NavLink>}
        </nav>
        <div className="sidebar-foot">
          {token ? (
            <button
              className="ghost"
              onClick={() => {
                logout();
                navigate("/");
              }}
            >
              Sign out
            </button>
          ) : (
            <button className="ghost" onClick={() => navigate("/login")}>
              Sign in
            </button>
          )}
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
