import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { userApi } from "../lib/services";
import type { Page, SubmissionListItem, UserProfile } from "../lib/types";
import { LANGUAGE_LABELS } from "../lib/types";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { VerdictBadge } from "../components/VerdictBadge";
import { Pagination } from "../components/Pagination";
import { Spinner, ErrorBox } from "../components/Spinner";
import { formatDateTime, ratingColorClass } from "../lib/format";

export function Profile() {
  const { username = "" } = useParams();
  const { user } = useAuth();
  const isMe = user?.username === username;

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [subs, setSubs] = useState<Page<SubmissionListItem> | null>(null);
  const [subPage, setSubPage] = useState(0);

  // Profile editing (owner only)
  const [editing, setEditing] = useState(false);
  const [avatarInput, setAvatarInput] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  async function saveProfile() {
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await userApi.updateMe(avatarInput.trim());
      setProfile(updated);
      setEditing(false);
    } catch (e) {
      setSaveError(e instanceof ApiError ? e.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  }

  useEffect(() => {
    setLoading(true);
    setError(null);
    userApi
      .get(username)
      .then(setProfile)
      .catch((e) => setError(e instanceof ApiError ? e.message : "User not found"))
      .finally(() => setLoading(false));
  }, [username]);

  useEffect(() => {
    userApi
      .submissions(username, { page: subPage, size: 15, sort: "submittedAt,desc" })
      .then(setSubs)
      .catch(() => setSubs(null));
  }, [username, subPage]);

  if (loading) return <Spinner />;
  if (error) return <ErrorBox message={error} />;
  if (!profile) return null;

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-[260px_1fr]">
      <aside className="cf-panel h-fit">
        <div className="cf-panel-title">Profile</div>
        <div className="p-4 text-center">
          {profile.avatarUrl && (
            <img
              src={profile.avatarUrl}
              alt={profile.username}
              className="mx-auto mb-3 h-20 w-20 rounded border border-cf-border object-cover"
              onError={(e) => (e.currentTarget.style.display = "none")}
            />
          )}
          <div
            className={`text-[22px] font-bold ${ratingColorClass(profile.rating)}`}
          >
            {profile.username}
          </div>
          <div className="mt-2 text-[12px] text-gray-600">{profile.role}</div>
          <div className="mt-3 text-[13px]">
            Rating:{" "}
            <span className={`font-bold ${ratingColorClass(profile.rating)}`}>
              {profile.rating}
            </span>
          </div>
          <div className="text-[13px]">
            Max rating:{" "}
            <span className={`font-bold ${ratingColorClass(profile.maxRating)}`}>
              {profile.maxRating}
            </span>
          </div>
          {isMe && (
            <div className="mt-2 text-[12px] text-gray-500">{profile.email}</div>
          )}
          <div className="mt-3 text-[11px] text-gray-400">
            Joined {formatDateTime(profile.createdAt)}
          </div>

          {isMe && !editing && (
            <button
              className="cf-btn mt-3"
              onClick={() => {
                setAvatarInput(profile.avatarUrl ?? "");
                setSaveError(null);
                setEditing(true);
              }}
            >
              Edit profile
            </button>
          )}
          {isMe && editing && (
            <div className="mt-3 space-y-2 border-t border-cf-border pt-3 text-left">
              {saveError && <ErrorBox message={saveError} />}
              <label className="cf-label">Avatar URL</label>
              <input
                className="cf-input"
                value={avatarInput}
                onChange={(e) => setAvatarInput(e.target.value)}
                placeholder="https://…/avatar.png"
              />
              <div className="flex gap-2">
                <button className="cf-btn cf-btn-primary" onClick={saveProfile} disabled={saving}>
                  {saving ? "Saving…" : "Save"}
                </button>
                <button className="cf-btn" onClick={() => setEditing(false)} disabled={saving}>
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </aside>

      <div className="cf-panel">
        <div className="cf-panel-title">Recent submissions</div>
        <div className="p-2">
          {!subs || subs.content.length === 0 ? (
            <p className="p-4 text-gray-500">No submissions.</p>
          ) : (
            <>
              <table className="cf-table">
                <thead>
                  <tr>
                    <th className="w-16">#</th>
                    <th>Problem</th>
                    <th className="w-24">Lang</th>
                    <th className="w-36">Verdict</th>
                    <th className="w-40">When</th>
                  </tr>
                </thead>
                <tbody>
                  {subs.content.map((s) => (
                    <tr key={s.id}>
                      <td>
                        <Link to={`/submissions/${s.id}`} className="font-mono">
                          {s.id}
                        </Link>
                      </td>
                      <td>{s.problemTitle}</td>
                      <td className="text-[12px]">
                        {LANGUAGE_LABELS[s.language] ?? s.language}
                      </td>
                      <td>
                        <VerdictBadge verdict={s.verdict} />
                      </td>
                      <td className="text-[12px] text-gray-600">
                        {formatDateTime(s.submittedAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination
                page={subs.number}
                totalPages={subs.totalPages}
                onChange={setSubPage}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
