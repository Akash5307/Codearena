import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../store/auth";
import { ApiError } from "../lib/api";
import { ErrorBox } from "../components/Spinner";

export function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function validate(): string | null {
    if (username.trim().length < 3 || username.trim().length > 30)
      return "Username must be 3–30 characters.";
    if (!/^\S+@\S+\.\S+$/.test(email)) return "Enter a valid email.";
    if (password.length < 6) return "Password must be at least 6 characters.";
    return null;
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const v = validate();
    if (v) {
      setError(v);
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      await register(username.trim(), email.trim(), password);
      navigate("/problems", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Registration failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-[380px]">
      <div className="cf-panel">
        <div className="cf-panel-title">Register</div>
        <form onSubmit={onSubmit} className="space-y-3 p-4">
          {error && <ErrorBox message={error} />}
          <div>
            <label className="cf-label">Handle</label>
            <input
              className="cf-input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
              required
            />
          </div>
          <div>
            <label className="cf-label">Email</label>
            <input
              type="email"
              className="cf-input"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
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
            {submitting ? "Creating account…" : "Register"}
          </button>
          <p className="text-center text-gray-600">
            Already registered?{" "}
            <Link to="/login" className="font-bold">
              Enter
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
