export type ReviewStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type IssueSeverity = "HIGH" | "MEDIUM" | "LOW";

export interface Issue {
  id: number | null;
  severity: IssueSeverity;
  lineNumber: number | null;
  title: string;
  description: string;
  suggestion: string | null;
}

export interface Review {
  id: number | null;
  language: string;
  status: ReviewStatus;
  score: number | null;
  aiSummary: string | null;
  codeSnippet: string | null;
  issues: Issue[] | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AiFix {
  id: number | null;
  originalSnippet: string;
  fixedSnippet: string;
  applied: boolean;
}

export const LANGUAGES = [
  { id: "java", label: "Java", monaco: "java" },
  { id: "javascript", label: "JavaScript", monaco: "javascript" },
  { id: "typescript", label: "TypeScript", monaco: "typescript" },
  { id: "python", label: "Python", monaco: "python" },
  { id: "csharp", label: "C#", monaco: "csharp" },
  { id: "go", label: "Go", monaco: "go" },
  { id: "kotlin", label: "Kotlin", monaco: "kotlin" },
  { id: "sql", label: "SQL", monaco: "sql" },
] as const;

export function monacoLanguage(id: string): string {
  return LANGUAGES.find((l) => l.id === id)?.monaco ?? "plaintext";
}

export const GUEST_REVIEW_KEY = "scrutiny.guestReview";

export function isPersistedReview(review: Review): review is Review & { id: number } {
  return review.id != null;
}
