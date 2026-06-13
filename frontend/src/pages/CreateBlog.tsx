import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { blogApi } from "../lib/services";
import { ApiError } from "../lib/api";
import { useAuth } from "../store/auth";
import { Spinner, ErrorBox } from "../components/Spinner";

// Handles both "new post" (/blogs/new) and "edit post" (/blogs/:id/edit).
export function CreateBlog() {
  const { id } = useParams();
  const editingId = id ? Number(id) : null;
  const { user } = useAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(editingId != null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (editingId == null) return;
    setLoading(true);
    blogApi
      .get(editingId)
      .then((post) => {
        // Only the author may edit; bounce others (server enforces this too).
        if (user && post.authorUsername !== user.username) {
          setError("You can only edit your own posts.");
        } else {
          setTitle(post.title);
          setContent(post.content);
        }
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : "Failed to load post"))
      .finally(() => setLoading(false));
  }, [editingId, user]);

  if (!user) {
    return (
      <div className="cf-panel p-8 text-center text-gray-600">
        <Link to="/login" className="font-bold">Enter</Link> to write a post.
      </div>
    );
  }

  if (loading) return <Spinner />;

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("Title and content are required.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const post =
        editingId != null
          ? await blogApi.update(editingId, { title: title.trim(), content: content.trim() })
          : await blogApi.create({ title: title.trim(), content: content.trim() });
      navigate(`/blogs/${post.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save post");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-[760px]">
      <div className="cf-panel">
        <div className="cf-panel-title flex items-center justify-between">
          <span>{editingId != null ? "Edit post" : "New post"}</span>
          <Link to="/blogs" className="text-[12px] font-normal text-white/90 hover:underline">
            ← back to blog
          </Link>
        </div>
        <form onSubmit={onSubmit} className="space-y-3 p-4">
          {error && <ErrorBox message={error} />}
          <div>
            <label className="cf-label">Title</label>
            <input className="cf-input" value={title} onChange={(e) => setTitle(e.target.value)} autoFocus required />
          </div>
          <div>
            <label className="cf-label">Content (Markdown)</label>
            <textarea
              className="cf-input font-mono text-[13px]"
              rows={14}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Write your post. Markdown + $LaTeX$ supported."
              required
            />
          </div>
          <button type="submit" className="cf-btn cf-btn-primary" disabled={submitting}>
            {submitting ? "Saving…" : editingId != null ? "Save changes" : "Publish post"}
          </button>
        </form>
      </div>
    </div>
  );
}
