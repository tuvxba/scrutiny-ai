import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { LANGUAGES, type Review } from "../types";

export function HistoryPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [language, setLanguage] = useState("");
  const [rows, setRows] = useState<Review[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const size = 8;

  useEffect(() => {
    let cancelled = false;
    api
      .listReviews(page, size, language || undefined)
      .then((data) => {
        if (cancelled) return;
        setRows(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "Could not load history");
      });
    return () => {
      cancelled = true;
    };
  }, [page, language]);

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Review history</h2>
          <p>Paginated archive of past submissions. Filter by language.</p>
        </div>
        <div className="filters">
          <select
            className="select"
            value={language}
            onChange={(e) => {
              setPage(0);
              setLanguage(e.target.value);
            }}
          >
            <option value="">All languages</option>
            {LANGUAGES.map((lang) => (
              <option key={lang.id} value={lang.id}>
                {lang.label}
              </option>
            ))}
          </select>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Language</th>
              <th>Status</th>
              <th>Score</th>
              <th>Summary</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan={5} className="empty">
                  No reviews yet. Run one from the editor.
                </td>
              </tr>
            )}
            {rows.map((row) => (
              <tr key={row.id ?? row.language} onClick={() => row.id != null && navigate(`/reviews/${row.id}`)}>
                <td className="mono">#{row.id}</td>
                <td>{row.language}</td>
                <td>
                  <span className={`badge ${row.status}`}>{row.status}</span>
                </td>
                <td className="mono">{row.score ?? "—"}</td>
                <td style={{ color: "var(--muted)" }}>
                  {row.aiSummary ? truncate(row.aiSummary, 90) : "—"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <div className="pager">
          <span>
            {totalElements} review{totalElements === 1 ? "" : "s"} · page {page + 1} of{" "}
            {Math.max(totalPages, 1)}
          </span>
          <div style={{ display: "flex", gap: 8 }}>
            <button className="ghost" style={{ width: "auto" }} disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </button>
            <button
              className="ghost"
              style={{ width: "auto" }}
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

function truncate(value: string, max: number): string {
  return value.length > max ? `${value.slice(0, max)}…` : value;
}
