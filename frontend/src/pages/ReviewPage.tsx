import Editor, { DiffEditor, type OnMount } from "@monaco-editor/react";
import { FlaskConical, Sparkles, WandSparkles, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import type { AiFix, Issue, Review } from "../types";
import { GUEST_REVIEW_KEY, isPersistedReview, monacoLanguage } from "../types";
import { useNavigate } from "react-router-dom";

type CodeEditor = {
  deltaDecorations: (
    oldDecorations: string[],
    newDecorations: Array<{
      range: { startLineNumber: number; startColumn: number; endLineNumber: number; endColumn: number };
      options: { isWholeLine: boolean; className: string; glyphMarginClassName: string };
    }>,
  ) => string[];
  revealLineInCenter: (line: number) => void;
};

function issueKey(issue: Issue, index: number): string {
  return issue.id != null ? String(issue.id) : `guest-${index}`;
}

export function ReviewPage() {
  const { id } = useParams();
  const parsedId = id ? Number(id) : Number.NaN;
  const persistedId = Number.isFinite(parsedId) ? parsedId : null;
  const guestMode = persistedId == null;
  const [review, setReview] = useState<Review | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [explanations, setExplanations] = useState<Record<string, string>>({});
  const [loadingExplain, setLoadingExplain] = useState<string | null>(null);
  const [fix, setFix] = useState<AiFix | null>(null);
  const [fixKey, setFixKey] = useState<string | null>(null);
  const [tests, setTests] = useState<string | null>(null);
  const [busyFix, setBusyFix] = useState(false);
  const [busyTests, setBusyTests] = useState(false);
  const monacoRef = useRef<CodeEditor | null>(null);
  const decorations = useRef<string[]>([]);

  const navigate = useNavigate();
  const [applying, setApplying] = useState(false);

  async function applyFix() {
    if (!fix || !review) return;
    setApplying(true);
    try {
      const newReview = await api.createReview(review.language, fix.fixedSnippet);
      if (newReview.id != null) {
        navigate(`/reviews/${newReview.id}`);
      } else {
        sessionStorage.setItem(GUEST_REVIEW_KEY, JSON.stringify(newReview));
        navigate("/reviews/preview");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Apply failed");
    } finally {
      setApplying(false);
      setFix(null);
    }
  }

  useEffect(() => {
    let cancelled = false;
    let timer: number | undefined;

    if (guestMode) {
      try {
        const raw = sessionStorage.getItem(GUEST_REVIEW_KEY);
        if (!raw) {
          setError("This guest review is gone. Run a new one — guest results are not saved.");
          return;
        }
        setReview(JSON.parse(raw) as Review);
      } catch {
        setError("Could not restore the guest review.");
      }
      return;
    }

    async function load() {
      try {
        const data = await api.getReview(persistedId!);
        if (cancelled) return;
        setReview(data);
        if (data.status === "PENDING" || data.status === "PROCESSING") {
          timer = window.setTimeout(load, 1500);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load review");
      }
    }

    void load();
    return () => {
      cancelled = true;
      if (timer) window.clearTimeout(timer);
    };
  }, [guestMode, persistedId]);

  function highlight(line: number | null) {
    const editorInstance = monacoRef.current;
    if (!editorInstance || !line) return;
    decorations.current = editorInstance.deltaDecorations(decorations.current, [
      {
        range: {
          startLineNumber: line,
          startColumn: 1,
          endLineNumber: line,
          endColumn: 1,
        },
        options: {
          isWholeLine: true,
          className: "issue-line",
          glyphMarginClassName: "issue-glyph",
        },
      },
    ]);
    editorInstance.revealLineInCenter(line);
  }

  const onMount: OnMount = (editorInstance) => {
    monacoRef.current = editorInstance as unknown as CodeEditor;
    const issues = review?.issues ?? [];
    const issue = issues.find((item, index) => issueKey(item, index) === selectedKey);
    if (issue?.lineNumber) highlight(issue.lineNumber);
  };

  async function explain(issue: Issue, key: string) {
    if (!review) return;
    setLoadingExplain(key);
    try {
      const res =
        isPersistedReview(review) && issue.id != null
          ? await api.explainIssue(review.id, issue.id)
          : await api.guestExplain(review, issue);
      setExplanations((prev) => ({ ...prev, [key]: res.explanation }));
    } catch (err) {
      setExplanations((prev) => ({
        ...prev,
        [key]: err instanceof Error ? err.message : "Explain failed",
      }));
    } finally {
      setLoadingExplain(null);
    }
  }

  async function fixWithAi(issue: Issue, key: string) {
    if (!review) return;
    setBusyFix(true);
    setFixKey(key);
    try {
      const result =
        isPersistedReview(review) && issue.id != null
          ? await api.fixIssue(review.id, issue.id)
          : await api.guestFix(review, issue);
      setFix(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Fix failed");
    } finally {
      setBusyFix(false);
    }
  }

  async function generateTests() {
    if (!review) return;
    setBusyTests(true);
    try {
      const res = isPersistedReview(review)
        ? await api.generateTests(review.id)
        : await api.guestGenerateTests(review);
      setTests(res.testCode);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Test generation failed");
    } finally {
      setBusyTests(false);
    }
  }

  if (error && !review) {
    return (
      <p className="error">
        {error} <Link to="/review">Start a new review</Link>
      </p>
    );
  }
  if (!review) return <p style={{ color: "var(--muted)" }}>Loading review…</p>;

  const issues = review.issues ?? [];
  const pending = review.status === "PENDING" || review.status === "PROCESSING";
  const persisted = isPersistedReview(review);

  return (
    <>
      <div className="page-head">
        <div>
          <h2>{persisted ? `Review #${review.id}` : "Guest review"}</h2>
          <p>
            {review.language} · {issues.length} issue{issues.length === 1 ? "" : "s"}
          </p>
        </div>
        <button className="btn secondary" onClick={generateTests} disabled={busyTests || pending}>
          <FlaskConical size={16} />
          {busyTests ? "Generating…" : "Generate Tests"}
        </button>
      </div>

      {!persisted && (
        <div className="card status-banner">
          This result is not stored. Sign in before the next run to keep history. Explain, fix, and
          tests still work on this page.
        </div>
      )}
      {pending && (
        <div className="card status-banner">
          <span className="pulse" />
          Analysis in progress. This page refreshes automatically.
        </div>
      )}
      {review.status === "FAILED" && (
        <div className="card status-banner">
          Review failed. Submit the snippet again or check the API logs.
        </div>
      )}
      {error && <p className="error">{error}</p>}

      <div className="review-grid">
        <div>
          <div className="card editor-wrap">
            <Editor
              height="620px"
              theme="vs-dark"
              language={monacoLanguage(review.language)}
              value={review.codeSnippet ?? ""}
              onMount={onMount}
              options={{
                readOnly: true,
                minimap: { enabled: false },
                fontSize: 14,
                fontFamily: "IBM Plex Mono, ui-monospace, monospace",
                glyphMargin: true,
                padding: { top: 16 },
                scrollBeyondLastLine: false,
                automaticLayout: true,
              }}
            />
          </div>
          {tests && (
            <div className="card" style={{ marginTop: 18, overflow: "hidden" }}>
              <div className="editor-toolbar">
                <strong>Generated tests</strong>
                <button className="ghost" style={{ width: "auto" }} onClick={() => setTests(null)}>
                  Close
                </button>
              </div>
              <Editor
                height="360px"
                theme="vs-dark"
                language="java"
                value={tests}
                options={{
                  readOnly: true,
                  minimap: { enabled: false },
                  fontSize: 13,
                  fontFamily: "IBM Plex Mono, ui-monospace, monospace",
                }}
              />
            </div>
          )}
        </div>

        <div>
          <div className="card score-card" style={{ marginBottom: 14 }}>
            <ScoreRing score={review.score} />
            <div className="score-meta">
              <span className={`badge ${review.status}`}>{review.status}</span>
              <h3>{review.score == null ? "Awaiting score" : "Quality score"}</h3>
              <p style={{ margin: 0, color: "var(--muted)" }}>
                {review.aiSummary || "The model summary appears here once analysis finishes."}
              </p>
            </div>
          </div>
          <div className="issues">
            {issues.length === 0 && !pending && (
              <div className="card empty">No issues reported for this snippet.</div>
            )}
            {issues.map((issue, index) => {
              const key = issueKey(issue, index);
              return (
                <article
                  key={key}
                  className={`card issue ${selectedKey === key ? "selected" : ""}`}
                  onClick={() => {
                    setSelectedKey(key);
                    highlight(issue.lineNumber);
                  }}
                >
                  <div className="issue-top">
                    <span className={`badge ${issue.severity}`}>{issue.severity}</span>
                    {issue.lineNumber != null && (
                      <span className="badge mono">line {issue.lineNumber}</span>
                    )}
                  </div>
                  <h4>{issue.title}</h4>
                  <p>{issue.description}</p>
                  {issue.suggestion && (
                    <p style={{ marginTop: 8 }}>
                      <strong>Suggestion:</strong> {issue.suggestion}
                    </p>
                  )}
                  <div className="issue-actions">
                    <button
                      className="btn secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        void explain(issue, key);
                      }}
                      disabled={loadingExplain === key}
                    >
                      <Sparkles size={15} />
                      {loadingExplain === key ? "Explaining…" : "Explain"}
                    </button>
                    <button
                      className="btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        void fixWithAi(issue, key);
                      }}
                      disabled={busyFix && fixKey === key}
                    >
                      <WandSparkles size={15} />
                      {busyFix && fixKey === key ? "Fixing…" : "Fix with AI"}
                    </button>
                  </div>
                  {explanations[key] && <div className="explanation">{explanations[key]}</div>}
                </article>
              );
            })}
          </div>
        </div>
      </div>

      {fix && (
        <div className="modal-backdrop" onClick={() => setFix(null)}>
          <div className="card modal" onClick={(e) => e.stopPropagation()}>
            <header>
              <h3>Fix with AI · before / after</h3>
              <div style={{ display: "flex", gap: 8 }}>
                <button className="btn" onClick={applyFix} disabled={applying}>
                  {applying ? "Applying…" : "Apply & Re-review"}
                </button>
                <button className="ghost" style={{ width: "auto" }} onClick={() => setFix(null)}>
                  <X size={16} />
                </button>
              </div>
            </header>
            <div className="diff-wrap">
              <DiffEditor
                original={fix.originalSnippet}
                modified={fix.fixedSnippet}
                language={monacoLanguage(review.language)}
                theme="vs-dark"
                options={{
                  readOnly: true,
                  renderSideBySide: true,
                  minimap: { enabled: false },
                  fontFamily: "IBM Plex Mono, ui-monospace, monospace",
                }}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function ScoreRing({ score }: { score: number | null }) {
  const value = score ?? 0;
  const r = 46;
  const c = 2 * Math.PI * r;
  const offset = c - (value / 100) * c;
  const color = value >= 80 ? "#5ee0b5" : value >= 50 ? "#e8a54b" : "#ff6b7a";

  return (
    <svg className="score-ring" viewBox="0 0 108 108">
      <circle cx="54" cy="54" r={r} fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="8" />
      <circle
        cx="54"
        cy="54"
        r={r}
        fill="none"
        stroke={color}
        strokeWidth="8"
        strokeDasharray={c}
        strokeDashoffset={score == null ? c : offset}
        strokeLinecap="round"
        transform="rotate(-90 54 54)"
      />
      <text x="54" y="60" textAnchor="middle" fill="#efe8dc" fontSize="22" fontFamily="Fraunces, serif">
        {score ?? "—"}
      </text>
    </svg>
  );
}
