import Editor from "@monaco-editor/react";
import { Play } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../auth";
import { GUEST_REVIEW_KEY, LANGUAGES, monacoLanguage } from "../types";

const SAMPLE = `public class OrderService {
    public double total(List<Order> orders) {
        double sum = 0;
        for (int i = 0; i <= orders.size(); i++) {
            sum += orders.get(i).getPrice();
        }
        return sum;
    }
}
`;

export function NewReviewPage() {
  const navigate = useNavigate();
  const { token } = useAuth();
  const [language, setLanguage] = useState("java");
  const [code, setCode] = useState(SAMPLE);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit() {
    if (!code.trim()) {
      setError("Paste some code first.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const review = await api.createReview(language, code);
      if (review.id != null) {
        navigate(`/reviews/${review.id}`);
      } else {
        sessionStorage.setItem(GUEST_REVIEW_KEY, JSON.stringify(review));
        navigate("/reviews/preview");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start review");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h2>New review</h2>
          <p>
            {token
              ? "Paste a snippet. Completed reviews are saved to your history."
              : "No account needed. Guest reviews are not saved — sign in if you want history."}
          </p>
        </div>
        <button className="btn" onClick={submit} disabled={busy}>
          <Play size={16} />
          {busy ? (token ? "Submitting…" : "Analyzing…") : "Run review"}
        </button>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="card editor-wrap">
        <div className="editor-toolbar">
          <select
            className="select"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
          >
            {LANGUAGES.map((lang) => (
              <option key={lang.id} value={lang.id}>
                {lang.label}
              </option>
            ))}
          </select>
          <span className="mono" style={{ color: "var(--muted)" }}>
            Monaco · syntax highlighting
          </span>
        </div>
        <Editor
          height="560px"
          theme="vs-dark"
          language={monacoLanguage(language)}
          value={code}
          onChange={(value) => setCode(value ?? "")}
          options={{
            minimap: { enabled: false },
            fontSize: 14,
            fontFamily: "IBM Plex Mono, ui-monospace, monospace",
            padding: { top: 16 },
            scrollBeyondLastLine: false,
            automaticLayout: true,
          }}
        />
      </div>
    </>
  );
}
