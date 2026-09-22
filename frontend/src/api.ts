import type { AiFix, Issue, PageResponse, Review } from "./types";

const TOKEN_KEY = "scrutiny.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

function isPublicCall(path: string, method: string): boolean {
  if (path.startsWith("/api/auth") || path.startsWith("/api/guest")) return true;
  return path === "/api/reviews" && method === "POST";
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(path, { ...init, headers });
  if (response.status === 401 && !isPublicCall(path, method)) {
    setToken(null);
    window.location.assign("/login");
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as Record<string, string>;
      message = body.error || body.email || body.password || body.name || Object.values(body)[0] || message;
    } catch {
      /* ignore */
    }
    throw new Error(message);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

function guestIssueBody(review: Review, issue: Issue) {
  return {
    language: review.language,
    codeSnippet: review.codeSnippet ?? "",
    severity: issue.severity,
    lineNumber: issue.lineNumber,
    title: issue.title,
    description: issue.description,
    suggestion: issue.suggestion,
  };
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string }>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),
  register: (name: string, email: string, password: string) =>
    request<{ token: string }>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password }),
    }),
  createReview: (language: string, codeSnippet: string) =>
    request<Review>("/api/reviews", {
      method: "POST",
      body: JSON.stringify({ language, codeSnippet }),
    }),
  getReview: (id: number) => request<Review>(`/api/reviews/${id}`),
  listReviews: (page: number, size: number, language?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (language) params.set("language", language);
    return request<PageResponse<Review>>(`/api/reviews?${params}`);
  },
  explainIssue: (reviewId: number, issueId: number) =>
    request<{ explanation: string }>(`/api/reviews/${reviewId}/issues/${issueId}/explain`, {
      method: "POST",
    }),
  fixIssue: (reviewId: number, issueId: number) =>
    request<AiFix>(`/api/reviews/${reviewId}/issues/${issueId}/fix`, { method: "POST" }),
  generateTests: (reviewId: number) =>
    request<{ id: number; testCode: string }>(`/api/reviews/${reviewId}/generate-tests`, {
      method: "POST",
    }),
  guestExplain: (review: Review, issue: Issue) =>
    request<{ explanation: string }>("/api/guest/explain", {
      method: "POST",
      body: JSON.stringify(guestIssueBody(review, issue)),
    }),
  guestFix: (review: Review, issue: Issue) =>
    request<AiFix>("/api/guest/fix", {
      method: "POST",
      body: JSON.stringify(guestIssueBody(review, issue)),
    }),
  guestGenerateTests: (review: Review) =>
    request<{ id: number | null; testCode: string }>("/api/guest/generate-tests", {
      method: "POST",
      body: JSON.stringify({ language: review.language, codeSnippet: review.codeSnippet ?? "" }),
    }),
};
