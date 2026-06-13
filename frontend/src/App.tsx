import { Navigate, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { Login } from "./pages/Login";
import { Register } from "./pages/Register";
import { ProblemList } from "./pages/ProblemList";
import { ProblemDetail } from "./pages/ProblemDetail";
import { CreateProblem } from "./pages/CreateProblem";
import { ContestList } from "./pages/ContestList";
import { ContestDetail } from "./pages/ContestDetail";
import { CreateContest } from "./pages/CreateContest";
import { BlogList } from "./pages/BlogList";
import { BlogDetail } from "./pages/BlogDetail";
import { CreateBlog } from "./pages/CreateBlog";
import { Status } from "./pages/Status";
import { SubmissionDetail } from "./pages/SubmissionDetail";
import { Profile } from "./pages/Profile";
import { Ratings } from "./pages/Ratings";

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/problems" replace />} />
        <Route path="/problems" element={<ProblemList />} />
        <Route
          path="/problems/new"
          element={
            <ProtectedRoute>
              <CreateProblem />
            </ProtectedRoute>
          }
        />
        <Route path="/problems/:slug" element={<ProblemDetail />} />
        <Route path="/contests" element={<ContestList />} />
        <Route
          path="/contests/new"
          element={
            <ProtectedRoute>
              <CreateContest />
            </ProtectedRoute>
          }
        />
        <Route path="/contests/:slug" element={<ContestDetail />} />
        <Route
          path="/blogs/new"
          element={
            <ProtectedRoute>
              <CreateBlog />
            </ProtectedRoute>
          }
        />
        <Route
          path="/blogs/:id/edit"
          element={
            <ProtectedRoute>
              <CreateBlog />
            </ProtectedRoute>
          }
        />
        <Route path="/blogs" element={<BlogList />} />
        <Route path="/blogs/:id" element={<BlogDetail />} />
        <Route path="/status" element={<Status />} />
        <Route path="/ratings" element={<Ratings />} />
        <Route path="/users/:username" element={<Profile />} />
        <Route
          path="/submissions/:id"
          element={
            <ProtectedRoute>
              <SubmissionDetail />
            </ProtectedRoute>
          }
        />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}

function NotFound() {
  return (
    <div className="cf-panel p-8 text-center">
      <div className="text-[40px] font-bold text-cf-blue">404</div>
      <p className="mt-2 text-gray-600">Page not found.</p>
    </div>
  );
}
