import type { ReactNode } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./auth";
import { AppShell } from "./AppShell";
import { AuthPage } from "./pages/AuthPage";
import { HomePage } from "./pages/HomePage";
import { HistoryPage } from "./pages/HistoryPage";
import { NewReviewPage } from "./pages/NewReviewPage";
import { ReviewPage } from "./pages/ReviewPage";

function RequireAuth({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<AuthPage />} />
      <Route element={<AppShell />}>
        <Route path="/review" element={<NewReviewPage />} />
        <Route path="/reviews/preview" element={<ReviewPage />} />
        <Route path="/reviews/:id" element={<ReviewPage />} />
        <Route
          path="/history"
          element={
            <RequireAuth>
              <HistoryPage />
            </RequireAuth>
          }
        />
      </Route>
    </Routes>
  );
}
