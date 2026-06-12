import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../store/auth";
import { ApiError } from "../lib/api";
import { ErrorBox } from "../components/Spinner";

export function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string })?.from ?? "/problems";

  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(usernameOrEmail.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-[380px]">
      <div className="cf-panel">
        <div className="cf-panel-title">Enter</div>
        <form onSubmit={onSubmit} className="space-y-3 p-4">
          {error && <ErrorBox message={error} />}
          <div>
            <label className="cf-label">Handle or email</label>
            <input
              className="cf-input"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              autoFocus
              required
            />
          </div>
          <div>
            <label className="cf-label">Password</label>
            <input
              type="password"
              className="cf-input"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="cf-btn cf-btn-primary w-full" disabled={submitting}>
            {submitting ? "Signing in…" : "Enter"}
          </button>
          <p className="text-center text-gray-600">
            No account?{" "}
            <Link to="/register" className="font-bold">
              Register
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
