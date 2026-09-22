import { ArrowRight, History, LogIn, Play, ScanSearch } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth";

export function HomePage() {
  const { token } = useAuth();

  return (
    <div className="auth-layout">
      <section className="auth-hero">
        <div className="brand">
          <div className="brand-mark">
            <ScanSearch size={20} />
          </div>
          <div>
            <h1>ScrutinyAI</h1>
            <p>Code review lab</p>
          </div>
        </div>
        <div>
          <h2>See the code as the reviewer sees it.</h2>
          <p>
            Paste a snippet, get a score and line-level findings. Explain, fix, and
            generate tests without an account — sign in when you want that work kept
            in history.
          </p>
        </div>
        <p className="mono">score · issues · explain · fix · tests</p>
      </section>
      <section className="auth-panel">
        <div className="home-choices">
          <Link className="choice-card primary" to="/review">
            <span className="choice-icon">
              <Play size={20} />
            </span>
            <span>
              <strong>Start review</strong>
              <em>{token ? "Open the editor and paste a snippet." : "No account needed. Guest runs are not saved."}</em>
            </span>
            <ArrowRight size={18} />
          </Link>
          {token ? (
            <Link className="choice-card" to="/history">
              <span className="choice-icon">
                <History size={20} />
              </span>
              <span>
                <strong>Review history</strong>
                <em>Open saved scores, issues, and past snippets.</em>
              </span>
              <ArrowRight size={18} />
            </Link>
          ) : (
            <Link className="choice-card" to="/login">
              <span className="choice-icon">
                <LogIn size={20} />
              </span>
              <span>
                <strong>Sign in</strong>
                <em>Keep reviews in history and return to them later.</em>
              </span>
              <ArrowRight size={18} />
            </Link>
          )}
        </div>
      </section>
    </div>
  );
}
